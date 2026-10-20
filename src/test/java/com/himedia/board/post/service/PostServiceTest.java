package com.himedia.board.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.himedia.board.global.common.PageResult;
import com.himedia.board.global.exception.ForbiddenException;
import com.himedia.board.global.exception.NotFoundException;
import com.himedia.board.global.session.LoginMember;
import com.himedia.board.member.domain.Member;
import com.himedia.board.member.repository.MemberRepository;
import com.himedia.board.post.dto.PostDetailResponse;
import com.himedia.board.post.dto.PostForm;
import com.himedia.board.post.dto.PostListResponse;
import com.himedia.board.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PostServiceTest {

	@Autowired
	PostService postService;
	@Autowired
	PostRepository postRepository;
	@Autowired
	MemberRepository memberRepository;

	LoginMember writer;
	LoginMember other;

	@BeforeEach
	void setUp() {
		writer = saveMember("post-svc-w@test.com", "작성자");
		other = saveMember("post-svc-o@test.com", "다른사람");
	}

	private LoginMember saveMember(String email, String nickname) {
		Long id = memberRepository.save(
				Member.builder().email(email).password("hashed").nickname(nickname).build());
		return new LoginMember(id, email, nickname);
	}

	@Test
	void F05_작성자는_로그인한_회원으로_저장된다() {
		Long id = postService.write(writer, new PostForm("제목", "본문"));

		assertThat(postRepository.findById(id).orElseThrow().getMemberId()).isEqualTo(writer.id());
	}

	@Test
	void F03_음수_페이지는_0으로_처리한다() {
		postService.write(writer, new PostForm("제목", "본문"));

		PageResult<PostListResponse> page = postService.getPage(-3);

		assertThat(page.page()).isZero();
		assertThat(page.size()).isEqualTo(PostService.PAGE_SIZE);
		assertThat(page.content()).isNotEmpty();
		assertThat(page.hasPrevious()).isFalse();
	}

	@Test
	void F03_범위를_넘는_페이지는_빈_목록이다() {
		postService.write(writer, new PostForm("제목", "본문"));

		PageResult<PostListResponse> page = postService.getPage(100_000);

		assertThat(page.content()).isEmpty();
		assertThat(page.hasNext()).isFalse();
	}

	@Test
	void F04_상세_조회시_조회수가_1_증가하고_작성자_여부가_표시된다() {
		Long id = postService.write(writer, new PostForm("제목", "본문"));

		PostDetailResponse first = postService.getDetail(id, writer);
		PostDetailResponse second = postService.getDetail(id, other);
		PostDetailResponse guest = postService.getDetail(id, null);

		assertThat(first.viewCount()).isEqualTo(1);
		assertThat(second.viewCount()).isEqualTo(2);
		assertThat(first.owner()).isTrue();
		assertThat(second.owner()).isFalse();
		assertThat(guest.owner()).isFalse();
	}

	@Test
	void F04_없는_글은_NotFoundException() {
		assertThatThrownBy(() -> postService.getDetail(-1L, null)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void F06_작성자는_수정할_수_있다() {
		Long id = postService.write(writer, new PostForm("제목", "본문"));

		assertThat(postService.getEditForm(id, writer).getTitle()).isEqualTo("제목");
		postService.update(id, writer, new PostForm("새 제목", "새 본문"));

		assertThat(postRepository.findById(id).orElseThrow().getTitle()).isEqualTo("새 제목");
	}

	@Test
	void F06_작성자가_아니면_수정할_수_없다() {
		Long id = postService.write(writer, new PostForm("제목", "본문"));

		assertThatThrownBy(() -> postService.getEditForm(id, other)).isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> postService.update(id, other, new PostForm("x", "y")))
				.isInstanceOf(ForbiddenException.class);
		assertThat(postRepository.findById(id).orElseThrow().getTitle()).isEqualTo("제목");
	}

	@Test
	void F06_없는_글은_수정할_수_없다() {
		assertThatThrownBy(() -> postService.update(-1L, writer, new PostForm("x", "y")))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void F07_작성자는_삭제할_수_있다() {
		Long id = postService.write(writer, new PostForm("제목", "본문"));

		postService.delete(id, writer);

		assertThat(postRepository.findById(id)).isEmpty();
	}

	@Test
	void F07_작성자가_아니면_삭제할_수_없다() {
		Long id = postService.write(writer, new PostForm("제목", "본문"));

		assertThatThrownBy(() -> postService.delete(id, other)).isInstanceOf(ForbiddenException.class);
		assertThat(postRepository.findById(id)).isPresent();
	}
}

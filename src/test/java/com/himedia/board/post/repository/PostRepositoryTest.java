package com.himedia.board.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.himedia.board.comment.domain.Comment;
import com.himedia.board.comment.repository.CommentRepository;
import com.himedia.board.member.domain.Member;
import com.himedia.board.member.repository.MemberRepository;
import com.himedia.board.post.domain.Post;
import com.himedia.board.post.dto.PostDetailResponse;
import com.himedia.board.post.dto.PostListResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostRepository.class, MemberRepository.class, CommentRepository.class})
class PostRepositoryTest {

	@Autowired
	PostRepository postRepository;
	@Autowired
	MemberRepository memberRepository;
	@Autowired
	CommentRepository commentRepository;
	@Autowired
	JdbcTemplate jdbcTemplate;

	Long memberId;

	@BeforeEach
	void setUp() {
		memberId = memberRepository.save(
				Member.builder().email("post-repo@test.com").password("hashed").nickname("글작성자").build());
	}

	private Long savePost(String title) {
		return postRepository.save(Post.builder().memberId(memberId).title(title).content("본문").build());
	}

	private void saveComment(Long postId) {
		commentRepository.save(Comment.builder().postId(postId).memberId(memberId).content("댓글").build());
	}

	@Test
	void F05_게시글을_저장하면_생성된_id로_조회된다() {
		Long id = savePost("제목");

		Post post = postRepository.findById(id).orElseThrow();
		assertThat(post.getMemberId()).isEqualTo(memberId);
		assertThat(post.getTitle()).isEqualTo("제목");
		assertThat(post.getViewCount()).isZero();
	}

	@Test
	void F03_목록은_최신순이고_작성자_닉네임과_댓글수를_함께_가져온다() {
		Long first = savePost("첫 글");
		Long second = savePost("둘째 글");
		saveComment(first);
		saveComment(first);

		List<PostListResponse> page = postRepository.findPage(0, 10);

		assertThat(page).extracting(PostListResponse::id).containsSubsequence(second, first);
		PostListResponse firstRow = page.stream().filter(p -> p.id().equals(first)).findFirst().orElseThrow();
		assertThat(firstRow.commentCount()).isEqualTo(2);
		assertThat(firstRow.writerNickname()).isEqualTo("글작성자");
		assertThat(postRepository.count()).isGreaterThanOrEqualTo(2);
	}

	@Test
	void F03_LIMIT_OFFSET으로_페이지를_나눈다() {
		long before = postRepository.count();
		for (int i = 0; i < 3; i++) {
			savePost("글" + i);
		}

		assertThat(postRepository.count()).isEqualTo(before + 3);
		assertThat(postRepository.findPage(0, 2)).hasSize(2);
		assertThat(postRepository.findPage(before + 3, 10)).isEmpty();
	}

	@Test
	void F04_상세_조회와_조회수_증가는_수정일을_바꾸지_않는다() {
		Long id = savePost("상세");
		jdbcTemplate.update("UPDATE post SET updated_at = '2020-01-01 00:00:00', created_at = '2020-01-01 00:00:00' WHERE id = ?", id);

		assertThat(postRepository.increaseViewCount(id)).isEqualTo(1);

		PostDetailResponse detail = postRepository.findDetailById(id).orElseThrow();
		assertThat(detail.viewCount()).isEqualTo(1);
		assertThat(detail.writerNickname()).isEqualTo("글작성자");
		assertThat(detail.edited()).isFalse();
		assertThat(postRepository.increaseViewCount(-1L)).isZero();
	}

	@Test
	void F06_수정하면_제목_본문과_수정일이_바뀐다() {
		Long id = savePost("수정 전");
		jdbcTemplate.update("UPDATE post SET updated_at = '2020-01-01 00:00:00', created_at = '2020-01-01 00:00:00' WHERE id = ?", id);

		assertThat(postRepository.update(id, "수정 후", "새 본문")).isEqualTo(1);

		PostDetailResponse detail = postRepository.findDetailById(id).orElseThrow();
		assertThat(detail.title()).isEqualTo("수정 후");
		assertThat(detail.content()).isEqualTo("새 본문");
		assertThat(detail.edited()).isTrue();
	}

	@Test
	void F07_게시글을_삭제하면_댓글도_함께_삭제된다() {
		Long id = savePost("삭제");
		saveComment(id);

		assertThat(postRepository.deleteById(id)).isEqualTo(1);

		assertThat(postRepository.findById(id)).isEmpty();
		assertThat(commentRepository.findResponsesByPostId(id)).isEmpty();
	}
}

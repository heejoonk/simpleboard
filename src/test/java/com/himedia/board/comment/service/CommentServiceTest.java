package com.himedia.board.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.himedia.board.comment.dto.CommentForm;
import com.himedia.board.comment.dto.CommentResponse;
import com.himedia.board.comment.repository.CommentRepository;
import com.himedia.board.global.exception.ForbiddenException;
import com.himedia.board.global.exception.NotFoundException;
import com.himedia.board.global.session.LoginMember;
import com.himedia.board.member.domain.Member;
import com.himedia.board.member.repository.MemberRepository;
import com.himedia.board.post.domain.Post;
import com.himedia.board.post.repository.PostRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CommentServiceTest {

	@Autowired
	CommentService commentService;
	@Autowired
	CommentRepository commentRepository;
	@Autowired
	PostRepository postRepository;
	@Autowired
	MemberRepository memberRepository;

	LoginMember writer;
	LoginMember other;
	Long postId;

	@BeforeEach
	void setUp() {
		writer = saveMember("cmt-svc-w@test.com", "댓글작성자");
		other = saveMember("cmt-svc-o@test.com", "다른사람");
		postId = savePost();
	}

	private LoginMember saveMember(String email, String nickname) {
		Long id = memberRepository.save(
				Member.builder().email(email).password("hashed").nickname(nickname).build());
		return new LoginMember(id, email, nickname);
	}

	private Long savePost() {
		return postRepository.save(Post.builder().memberId(writer.id()).title("글").content("본문").build());
	}

	@Test
	void F08_댓글을_작성하면_작성순으로_조회되고_본인_여부가_표시된다() {
		Long first = commentService.write(postId, writer, new CommentForm("첫 댓글"));
		Long second = commentService.write(postId, other, new CommentForm("둘째 댓글"));

		List<CommentResponse> comments = commentService.getComments(postId, writer);

		assertThat(comments).extracting(CommentResponse::id).containsExactly(first, second);
		assertThat(comments).extracting(CommentResponse::owner).containsExactly(true, false);
	}

	@Test
	void F08_없는_글에는_댓글을_작성할_수_없다() {
		assertThatThrownBy(() -> commentService.write(-1L, writer, new CommentForm("댓글")))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void F09_작성자는_댓글을_삭제할_수_있다() {
		Long commentId = commentService.write(postId, writer, new CommentForm("댓글"));

		commentService.delete(postId, commentId, writer);

		assertThat(commentRepository.findById(commentId)).isEmpty();
	}

	@Test
	void F09_작성자가_아니면_댓글을_삭제할_수_없다() {
		Long commentId = commentService.write(postId, writer, new CommentForm("댓글"));

		assertThatThrownBy(() -> commentService.delete(postId, commentId, other))
				.isInstanceOf(ForbiddenException.class);
		assertThat(commentRepository.findById(commentId)).isPresent();
	}

	@Test
	void F09_다른_글의_댓글이면_404() {
		Long commentId = commentService.write(postId, writer, new CommentForm("댓글"));
		Long otherPostId = savePost();

		assertThatThrownBy(() -> commentService.delete(otherPostId, commentId, writer))
				.isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> commentService.delete(postId, -1L, writer))
				.isInstanceOf(NotFoundException.class);
	}
}

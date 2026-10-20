package com.himedia.board.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.himedia.board.comment.domain.Comment;
import com.himedia.board.comment.dto.CommentResponse;
import com.himedia.board.member.domain.Member;
import com.himedia.board.member.repository.MemberRepository;
import com.himedia.board.post.domain.Post;
import com.himedia.board.post.repository.PostRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CommentRepository.class, PostRepository.class, MemberRepository.class})
class CommentRepositoryTest {

	@Autowired
	CommentRepository commentRepository;
	@Autowired
	PostRepository postRepository;
	@Autowired
	MemberRepository memberRepository;

	Long memberId;
	Long postId;

	@BeforeEach
	void setUp() {
		memberId = memberRepository.save(
				Member.builder().email("comment-repo@test.com").password("hashed").nickname("댓글러").build());
		postId = postRepository.save(Post.builder().memberId(memberId).title("글").content("본문").build());
	}

	private Long saveComment(String content) {
		return commentRepository.save(
				Comment.builder().postId(postId).memberId(memberId).content(content).build());
	}

	@Test
	void F08_댓글을_저장하고_조회한다() {
		Long id = saveComment("첫 댓글");

		Comment comment = commentRepository.findById(id).orElseThrow();
		assertThat(comment.getPostId()).isEqualTo(postId);
		assertThat(comment.getMemberId()).isEqualTo(memberId);
		assertThat(comment.getContent()).isEqualTo("첫 댓글");
	}

	@Test
	void F08_댓글_목록은_작성순이고_작성자_닉네임을_포함한다() {
		Long first = saveComment("1");
		Long second = saveComment("2");

		List<CommentResponse> comments = commentRepository.findResponsesByPostId(postId);

		assertThat(comments).extracting(CommentResponse::id).containsExactly(first, second);
		assertThat(comments.get(0).writerNickname()).isEqualTo("댓글러");
		assertThat(comments.get(0).writerId()).isEqualTo(memberId);
	}

	@Test
	void F09_댓글을_삭제한다() {
		Long id = saveComment("삭제할 댓글");

		assertThat(commentRepository.deleteById(id)).isEqualTo(1);
		assertThat(commentRepository.findById(id)).isEmpty();
	}
}

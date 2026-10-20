package com.himedia.board.comment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.himedia.board.comment.domain.Comment;
import com.himedia.board.comment.repository.CommentRepository;
import com.himedia.board.global.session.LoginMember;
import com.himedia.board.global.session.SessionConst;
import com.himedia.board.member.domain.Member;
import com.himedia.board.member.repository.MemberRepository;
import com.himedia.board.post.domain.Post;
import com.himedia.board.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommentControllerTest {

	@Autowired
	MockMvc mockMvc;
	@Autowired
	MemberRepository memberRepository;
	@Autowired
	PostRepository postRepository;
	@Autowired
	CommentRepository commentRepository;

	MockHttpSession writerSession;
	MockHttpSession otherSession;
	Long writerId;
	Long postId;

	@BeforeEach
	void setUp() {
		writerSession = loginSession("cmt-ctrl-w@test.com", "댓글주인");
		otherSession = loginSession("cmt-ctrl-o@test.com", "남의댓글");
		writerId = ((LoginMember) writerSession.getAttribute(SessionConst.LOGIN_MEMBER)).id();
		postId = postRepository.save(Post.builder().memberId(writerId).title("글").content("본문").build());
	}

	private MockHttpSession loginSession(String email, String nickname) {
		Long id = memberRepository.save(
				Member.builder().email(email).password("hashed").nickname(nickname).build());
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionConst.LOGIN_MEMBER, new LoginMember(id, email, nickname));
		return session;
	}

	private Long saveComment() {
		return commentRepository.save(
				Comment.builder().postId(postId).memberId(writerId).content("댓글").build());
	}

	// ---------- F-08 댓글 작성 ----------

	@Test
	void F08_비로그인은_댓글을_작성할_수_없다() throws Exception {
		mockMvc.perform(post("/posts/{postId}/comments", postId).param("content", "댓글"))
				.andExpect(redirectedUrl("/login"));
		assertThat(commentRepository.findResponsesByPostId(postId)).isEmpty();
	}

	@Test
	void F08_로그인하면_댓글을_작성하고_상세로_돌아온다() throws Exception {
		mockMvc.perform(post("/posts/{postId}/comments", postId).session(otherSession).param("content", "좋은 글"))
				.andExpect(redirectedUrl("/posts/" + postId));
		assertThat(commentRepository.findResponsesByPostId(postId)).hasSize(1);
	}

	@Test
	void F08_내용이_공백이거나_500자를_넘으면_flash_메시지와_함께_돌아온다() throws Exception {
		mockMvc.perform(post("/posts/{postId}/comments", postId).session(otherSession).param("content", "  "))
				.andExpect(redirectedUrl("/posts/" + postId))
				.andExpect(flash().attribute("errorMessage", "댓글은 1~500자로 입력해주세요"));
		mockMvc.perform(post("/posts/{postId}/comments", postId).session(otherSession)
						.param("content", "가".repeat(501)))
				.andExpect(flash().attribute("errorMessage", "댓글은 1~500자로 입력해주세요"));
		assertThat(commentRepository.findResponsesByPostId(postId)).isEmpty();
	}

	@Test
	void F08_없는_글에_댓글을_작성하면_404() throws Exception {
		mockMvc.perform(post("/posts/{postId}/comments", -1).session(otherSession).param("content", "댓글"))
				.andExpect(status().isNotFound());
	}

	// ---------- F-09 댓글 삭제 (비로그인 / 타인 / 본인) ----------

	@Test
	void F09_비로그인은_댓글을_삭제할_수_없다() throws Exception {
		Long commentId = saveComment();

		mockMvc.perform(post("/posts/{postId}/comments/{commentId}/delete", postId, commentId))
				.andExpect(redirectedUrl("/login"));
		assertThat(commentRepository.findById(commentId)).isPresent();
	}

	@Test
	void F09_작성자가_아니면_댓글을_삭제할_수_없다() throws Exception {
		Long commentId = saveComment();

		mockMvc.perform(post("/posts/{postId}/comments/{commentId}/delete", postId, commentId)
						.session(otherSession))
				.andExpect(status().isForbidden());
		assertThat(commentRepository.findById(commentId)).isPresent();
	}

	@Test
	void F09_작성자는_댓글을_삭제하고_상세로_돌아온다() throws Exception {
		Long commentId = saveComment();

		mockMvc.perform(post("/posts/{postId}/comments/{commentId}/delete", postId, commentId)
						.session(writerSession))
				.andExpect(redirectedUrl("/posts/" + postId));
		assertThat(commentRepository.findById(commentId)).isEmpty();
	}

	@Test
	void F09_댓글이_URL의_글에_속하지_않으면_404() throws Exception {
		Long commentId = saveComment();
		Long otherPostId = postRepository.save(
				Post.builder().memberId(writerId).title("다른 글").content("본문").build());

		mockMvc.perform(post("/posts/{postId}/comments/{commentId}/delete", otherPostId, commentId)
						.session(writerSession))
				.andExpect(status().isNotFound());
		assertThat(commentRepository.findById(commentId)).isPresent();
	}
}

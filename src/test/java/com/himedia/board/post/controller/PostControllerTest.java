package com.himedia.board.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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
class PostControllerTest {

	@Autowired
	MockMvc mockMvc;
	@Autowired
	MemberRepository memberRepository;
	@Autowired
	PostRepository postRepository;

	MockHttpSession writerSession;
	MockHttpSession otherSession;
	Long writerId;
	Long postId;

	@BeforeEach
	void setUp() {
		writerSession = loginSession("post-ctrl-w@test.com", "글주인");
		otherSession = loginSession("post-ctrl-o@test.com", "남의글");
		writerId = ((LoginMember) writerSession.getAttribute(SessionConst.LOGIN_MEMBER)).id();
		postId = postRepository.save(
				Post.builder().memberId(writerId).title("원래 제목").content("<b>본문</b>\n둘째 줄").build());
	}

	private MockHttpSession loginSession(String email, String nickname) {
		Long id = memberRepository.save(
				Member.builder().email(email).password("hashed").nickname(nickname).build());
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionConst.LOGIN_MEMBER, new LoginMember(id, email, nickname));
		return session;
	}

	// ---------- F-03 목록 ----------

	@Test
	void F03_목록은_비로그인도_볼_수_있고_글쓰기_버튼은_로그인시에만_보인다() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("post/list"))
				.andExpect(model().attributeExists("page"))
				.andExpect(content().string(containsString("원래 제목")))
				.andExpect(content().string(not(containsString("글쓰기</a>"))));

		mockMvc.perform(get("/posts").session(writerSession))
				.andExpect(content().string(containsString("글쓰기</a>")));
	}

	@Test
	void F03_음수나_범위를_넘는_페이지도_에러_없이_처리한다() throws Exception {
		mockMvc.perform(get("/posts").param("page", "-5"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/posts").param("page", "99999"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("등록된 글이 없습니다")));
	}

	// ---------- F-04 상세 ----------

	@Test
	void F04_상세는_비로그인도_볼_수_있고_HTML은_이스케이프된다() throws Exception {
		mockMvc.perform(get("/posts/{id}", postId))
				.andExpect(status().isOk())
				.andExpect(view().name("post/detail"))
				.andExpect(content().string(containsString("&lt;b&gt;본문&lt;/b&gt;")))
				.andExpect(content().string(containsString("로그인 후 댓글을 작성할 수 있습니다")))
				.andExpect(content().string(not(containsString("/edit\""))));
	}

	@Test
	void F04_작성자에게만_수정_삭제_버튼이_보인다() throws Exception {
		mockMvc.perform(get("/posts/{id}", postId).session(writerSession))
				.andExpect(content().string(containsString("/posts/" + postId + "/edit")));
		mockMvc.perform(get("/posts/{id}", postId).session(otherSession))
				.andExpect(content().string(not(containsString("/posts/" + postId + "/edit"))));
	}

	@Test
	void F04_없는_글은_404() throws Exception {
		mockMvc.perform(get("/posts/{id}", Long.MAX_VALUE))
				.andExpect(status().isNotFound())
				.andExpect(view().name("error/404"));
		// 숫자가 아닌 id는 공개 경로(/posts/{숫자})가 아니므로 로그인 상태에서만 404에 도달한다
		mockMvc.perform(get("/posts/abc").session(writerSession))
				.andExpect(status().isNotFound());
	}

	// ---------- F-05 작성 ----------

	@Test
	void F05_비로그인은_작성_폼에_접근할_수_없다() throws Exception {
		mockMvc.perform(get("/posts/new"))
				.andExpect(redirectedUrl("/login?redirectURL=%2Fposts%2Fnew"));
		mockMvc.perform(post("/posts").param("title", "t").param("content", "c"))
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	void F05_로그인하면_글을_작성하고_상세로_이동한다() throws Exception {
		mockMvc.perform(get("/posts/new").session(writerSession))
				.andExpect(status().isOk())
				.andExpect(view().name("post/form"));

		mockMvc.perform(post("/posts").session(writerSession).param("title", "새 글").param("content", "새 본문"))
				.andExpect(redirectedUrlPattern("/posts/*"));
	}

	@Test
	void F05_제목_본문이_공백이거나_너무_길면_폼을_다시_보여준다() throws Exception {
		mockMvc.perform(post("/posts").session(writerSession).param("title", "   ").param("content", ""))
				.andExpect(view().name("post/form"))
				.andExpect(model().attributeHasFieldErrors("postForm", "title", "content"));

		mockMvc.perform(post("/posts").session(writerSession)
						.param("title", "가".repeat(101)).param("content", "나".repeat(5001)))
				.andExpect(model().attributeHasFieldErrors("postForm", "title", "content"));
	}

	// ---------- F-06 수정 (비로그인 / 타인 / 본인) ----------

	@Test
	void F06_비로그인은_수정할_수_없다() throws Exception {
		mockMvc.perform(get("/posts/{id}/edit", postId))
				.andExpect(redirectedUrl("/login?redirectURL=%2Fposts%2F" + postId + "%2Fedit"));
		mockMvc.perform(post("/posts/{id}/edit", postId).param("title", "x").param("content", "y"))
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	void F06_작성자가_아니면_수정할_수_없다() throws Exception {
		mockMvc.perform(get("/posts/{id}/edit", postId).session(otherSession))
				.andExpect(status().isForbidden())
				.andExpect(view().name("error/403"));
		mockMvc.perform(post("/posts/{id}/edit", postId).session(otherSession)
						.param("title", "해킹").param("content", "해킹"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/posts/{id}/edit", postId).session(otherSession)
						.param("title", "").param("content", ""))
				.andExpect(status().isForbidden());

		assertThat(postRepository.findById(postId).orElseThrow().getTitle()).isEqualTo("원래 제목");
	}

	@Test
	void F06_작성자는_기존_값이_채워진_폼에서_수정할_수_있다() throws Exception {
		mockMvc.perform(get("/posts/{id}/edit", postId).session(writerSession))
				.andExpect(status().isOk())
				.andExpect(view().name("post/form"))
				.andExpect(content().string(containsString("value=\"원래 제목\"")));

		mockMvc.perform(post("/posts/{id}/edit", postId).session(writerSession)
						.param("title", "수정 제목").param("content", "수정 본문"))
				.andExpect(redirectedUrl("/posts/" + postId));

		assertThat(postRepository.findById(postId).orElseThrow().getTitle()).isEqualTo("수정 제목");
	}

	@Test
	void F06_없는_글은_404() throws Exception {
		mockMvc.perform(get("/posts/{id}/edit", -1).session(writerSession))
				.andExpect(status().isNotFound());
	}

	// ---------- F-07 삭제 (비로그인 / 타인 / 본인) ----------

	@Test
	void F07_비로그인은_삭제할_수_없다() throws Exception {
		mockMvc.perform(post("/posts/{id}/delete", postId))
				.andExpect(redirectedUrl("/login"));
		assertThat(postRepository.findById(postId)).isPresent();
	}

	@Test
	void F07_작성자가_아니면_삭제할_수_없다() throws Exception {
		mockMvc.perform(post("/posts/{id}/delete", postId).session(otherSession))
				.andExpect(status().isForbidden());
		assertThat(postRepository.findById(postId)).isPresent();
	}

	@Test
	void F07_작성자는_삭제하고_목록으로_이동한다() throws Exception {
		mockMvc.perform(post("/posts/{id}/delete", postId).session(writerSession))
				.andExpect(redirectedUrl("/posts"));
		assertThat(postRepository.findById(postId)).isEmpty();
	}

	@Test
	void F07_GET으로는_삭제되지_않는다() throws Exception {
		mockMvc.perform(get("/posts/{id}/delete", postId).session(writerSession))
				.andExpect(status().isMethodNotAllowed());
		assertThat(postRepository.findById(postId)).isPresent();
	}
}

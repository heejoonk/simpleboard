package com.himedia.board.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.himedia.board.global.session.LoginMember;
import com.himedia.board.global.session.SessionConst;
import com.himedia.board.member.domain.Member;
import com.himedia.board.member.repository.MemberRepository;
import jakarta.servlet.http.HttpSession;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginControllerTest {

	private static final String EMAIL = "login-ctrl@test.com";
	private static final String PASSWORD = "password1";

	@Autowired
	MockMvc mockMvc;
	@Autowired
	MemberRepository memberRepository;
	@Autowired
	PasswordEncoder passwordEncoder;

	Long memberId;

	@BeforeEach
	void setUp() {
		memberId = memberRepository.save(Member.builder()
				.email(EMAIL).password(passwordEncoder.encode(PASSWORD)).nickname("로그인러").build());
	}

	@Test
	void F02_로그인_폼은_redirectURL을_숨은_필드로_담는다() throws Exception {
		mockMvc.perform(get("/login").param("redirectURL", "/posts/new"))
				.andExpect(status().isOk())
				.andExpect(view().name("member/login"))
				.andExpect(content().string(Matchers.containsString("value=\"/posts/new\"")))
				.andExpect(content().string(Matchers.containsString("로그인이 필요한 페이지입니다")));

		mockMvc.perform(get("/login").param("joined", ""))
				.andExpect(content().string(Matchers.containsString("가입이 완료되었습니다. 로그인해주세요")));
	}

	@Test
	void F02_로그인_성공시_세션에_비밀번호_없이_저장하고_목록으로_이동한다() throws Exception {
		MvcResult result = mockMvc.perform(post("/login").param("email", EMAIL).param("password", PASSWORD))
				.andExpect(redirectedUrl("/"))
				.andReturn();

		HttpSession session = result.getRequest().getSession(false);
		assertThat(session).isNotNull();
		Object loginMember = session.getAttribute(SessionConst.LOGIN_MEMBER);
		assertThat(loginMember).isEqualTo(new LoginMember(memberId, EMAIL, "로그인러"));
	}

	@Test
	void F02_로그인_후_원래_페이지로_돌아간다() throws Exception {
		mockMvc.perform(post("/login").param("email", EMAIL).param("password", PASSWORD)
						.param("redirectURL", "/posts/new"))
				.andExpect(redirectedUrl("/posts/new"));
	}

	@Test
	void F02_외부_redirectURL은_무시하고_목록으로_이동한다() throws Exception {
		mockMvc.perform(post("/login").param("email", EMAIL).param("password", PASSWORD)
						.param("redirectURL", "//evil.com"))
				.andExpect(redirectedUrl("/"));
		mockMvc.perform(post("/login").param("email", EMAIL).param("password", PASSWORD)
						.param("redirectURL", "https://evil.com"))
				.andExpect(redirectedUrl("/"));
	}

	@Test
	void F02_로그인_실패시_같은_폼에_글로벌_오류를_보여준다() throws Exception {
		mockMvc.perform(post("/login").param("email", EMAIL).param("password", "wrongpass1"))
				.andExpect(status().isOk())
				.andExpect(view().name("member/login"))
				.andExpect(model().attributeHasErrors("loginForm"))
				.andExpect(content().string(Matchers.containsString("이메일 또는 비밀번호가 올바르지 않습니다")))
				.andExpect(content().string(Matchers.containsString("value=\"" + EMAIL + "\"")));
	}

	@Test
	void F02_빈_값이면_필드별_오류를_보여준다() throws Exception {
		mockMvc.perform(post("/login").param("email", "").param("password", ""))
				.andExpect(model().attributeHasFieldErrors("loginForm", "email", "password"))
				.andExpect(content().string(Matchers.containsString("이메일을 입력해주세요")))
				.andExpect(content().string(Matchers.containsString("비밀번호를 입력해주세요")));
	}

	@Test
	void F02_이미_로그인한_사용자가_로그인_페이지에_오면_목록으로_이동한다() throws Exception {
		mockMvc.perform(get("/login").session(loginSession()))
				.andExpect(redirectedUrl("/"));
	}

	@Test
	void F02_로그아웃은_POST로_세션을_종료하고_이후_로그인_필요_페이지에_접근할_수_없다() throws Exception {
		MockHttpSession session = loginSession();

		mockMvc.perform(post("/logout").session(session))
				.andExpect(redirectedUrl("/"));

		assertThat(session.isInvalid()).isTrue();
		mockMvc.perform(get("/posts/new").session(session))
				.andExpect(redirectedUrl("/login?redirectURL=%2Fposts%2Fnew"));
	}

	@Test
	void F02_GET_로그아웃은_동작하지_않는다() throws Exception {
		MockHttpSession session = loginSession();

		mockMvc.perform(get("/logout").session(session))
				.andExpect(status().isMethodNotAllowed());

		assertThat(session.isInvalid()).isFalse();
	}

	private MockHttpSession loginSession() {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionConst.LOGIN_MEMBER, new LoginMember(memberId, EMAIL, "로그인러"));
		return session;
	}
}

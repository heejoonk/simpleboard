package com.himedia.board.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberControllerTest {

	@Autowired
	MockMvc mockMvc;
	@Autowired
	MemberRepository memberRepository;

	private MockHttpServletRequestBuilder signup(String email, String password, String confirm, String nickname) {
		return post("/signup")
				.param("email", email)
				.param("password", password)
				.param("passwordConfirm", confirm)
				.param("nickname", nickname);
	}

	@Test
	void F01_회원가입_폼을_보여준다() throws Exception {
		mockMvc.perform(get("/signup"))
				.andExpect(status().isOk())
				.andExpect(view().name("member/signup"));
	}

	@Test
	void F01_정상_가입하면_로그인_페이지로_이동한다() throws Exception {
		mockMvc.perform(signup("ctrl-a@test.com", "password1", "password1", "컨트롤A"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?joined"));

		Member saved = memberRepository.findByEmail("ctrl-a@test.com").orElseThrow();
		assertThat(saved.getPassword()).startsWith("$2");
	}

	@Test
	void F01_검증_규칙_위반시_폼을_다시_보여준다() throws Exception {
		mockMvc.perform(signup("not-email", "short", "different", "a"))
				.andExpect(status().isOk())
				.andExpect(view().name("member/signup"))
				.andExpect(model().attributeHasFieldErrors("signupRequest",
						"email", "password", "passwordConfirm", "nickname"));

		mockMvc.perform(signup("", "onlyletters", "onlyletters", "12345678901"))
				.andExpect(model().attributeHasFieldErrors("signupRequest", "email", "password", "nickname"));
	}

	@Test
	void F01_비밀번호_확인이_다르면_오류를_표시한다() throws Exception {
		mockMvc.perform(signup("ctrl-b@test.com", "password1", "password2", "컨트롤B"))
				.andExpect(view().name("member/signup"))
				.andExpect(model().attributeHasFieldErrorCode("signupRequest", "passwordConfirm", "mismatch"));

		assertThat(memberRepository.existsByEmail("ctrl-b@test.com")).isFalse();
	}

	@Test
	void F01_이메일_닉네임이_중복되면_오류를_표시하고_입력값을_유지한다() throws Exception {
		mockMvc.perform(signup("ctrl-c@test.com", "password1", "password1", "컨트롤C"));

		mockMvc.perform(signup("ctrl-c@test.com", "password1", "password1", "컨트롤C"))
				.andExpect(view().name("member/signup"))
				.andExpect(model().attributeHasFieldErrorCode("signupRequest", "email", "duplicate"))
				.andExpect(model().attributeHasFieldErrorCode("signupRequest", "nickname", "duplicate"))
				.andExpect(content().string(containsString("value=\"ctrl-c@test.com\"")));
	}

	@Test
	void F01_이미_로그인한_사용자는_목록으로_이동한다() throws Exception {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionConst.LOGIN_MEMBER, new LoginMember(1L, "a@test.com", "닉네임"));

		mockMvc.perform(get("/signup").session(session))
				.andExpect(redirectedUrl("/"));
	}
}

package com.himedia.board.global.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import com.himedia.board.global.session.LoginMember;
import com.himedia.board.global.session.SessionConst;
import com.himedia.board.global.web.RedirectUrls;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

class LoginCheckInterceptorTest {

	LoginCheckInterceptor interceptor = new LoginCheckInterceptor();

	private MockHttpServletResponse handle(MockHttpServletRequest request, boolean expectedPass) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		assertThat(interceptor.preHandle(request, response, new Object())).isEqualTo(expectedPass);
		return response;
	}

	@Test
	void F02_GET_공개_경로는_비로그인도_통과한다() throws Exception {
		handle(new MockHttpServletRequest("GET", "/"), true);
		handle(new MockHttpServletRequest("GET", "/posts"), true);
		handle(new MockHttpServletRequest("GET", "/posts/12"), true);
	}

	@Test
	void F02_비로그인_GET은_원래_URI와_쿼리를_redirectURL로_담아_로그인으로_보낸다() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/posts/3/edit");
		request.setQueryString("a=1&b=2");

		MockHttpServletResponse response = handle(request, false);

		assertThat(response.getRedirectedUrl()).isEqualTo("/login?redirectURL=%2Fposts%2F3%2Fedit%3Fa%3D1%26b%3D2");
	}

	@Test
	void F02_비로그인_GET_글쓰기는_로그인이_필요하다() throws Exception {
		MockHttpServletResponse response = handle(new MockHttpServletRequest("GET", "/posts/new"), false);

		assertThat(response.getRedirectedUrl()).isEqualTo("/login?redirectURL=%2Fposts%2Fnew");
	}

	@Test
	void F10_비로그인_첨부_다운로드와_미리보기는_로그인이_필요하다() throws Exception {
		MockHttpServletResponse download = handle(new MockHttpServletRequest("GET", "/posts/3/files/7"), false);
		MockHttpServletResponse preview = handle(new MockHttpServletRequest("GET", "/posts/3/files/7/preview"), false);

		assertThat(download.getRedirectedUrl()).isEqualTo("/login?redirectURL=%2Fposts%2F3%2Ffiles%2F7");
		assertThat(preview.getRedirectedUrl()).isEqualTo("/login?redirectURL=%2Fposts%2F3%2Ffiles%2F7%2Fpreview");
	}

	@Test
	void F02_비로그인_POST는_redirectURL_없이_로그인으로_보낸다() throws Exception {
		MockHttpServletResponse response = handle(new MockHttpServletRequest("POST", "/posts"), false);

		assertThat(response.getRedirectedUrl()).isEqualTo("/login");
	}

	@Test
	void F02_로그인_상태면_통과한다() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/posts");
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionConst.LOGIN_MEMBER, new LoginMember(1L, "a@test.com", "닉네임"));
		request.setSession(session);

		handle(request, true);
	}

	@Test
	void F02_외부_URL로의_redirectURL은_차단한다() {
		assertThat(RedirectUrls.safe("/posts/1?page=2")).isEqualTo("/posts/1?page=2");
		assertThat(RedirectUrls.safe("//evil.com")).isEqualTo("/");
		assertThat(RedirectUrls.safe("https://evil.com")).isEqualTo("/");
		assertThat(RedirectUrls.safe("/\\evil.com")).isEqualTo("/");
		assertThat(RedirectUrls.safe("")).isEqualTo("/");
		assertThat(RedirectUrls.safe(null)).isEqualTo("/");
	}
}

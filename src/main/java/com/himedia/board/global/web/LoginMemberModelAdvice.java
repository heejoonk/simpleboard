package com.himedia.board.global.web;

import com.himedia.board.global.session.LoginMember;
import com.himedia.board.global.session.SessionConst;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 화면 Model에 loginMember(비로그인 시 null)를 넣어 헤더의 로그인 상태 표시에 사용한다.
 */
@ControllerAdvice
public class LoginMemberModelAdvice {

	@ModelAttribute("loginMember")
	public LoginMember loginMember(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return null;
		}
		return (LoginMember) session.getAttribute(SessionConst.LOGIN_MEMBER);
	}
}

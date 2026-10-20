package com.himedia.board.member.controller;

import com.himedia.board.global.session.LoginMember;
import com.himedia.board.global.session.SessionConst;
import com.himedia.board.global.web.RedirectUrls;
import com.himedia.board.member.dto.LoginForm;
import com.himedia.board.member.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
@RequiredArgsConstructor
public class LoginController {

	private static final String LOGIN_VIEW = "member/login";

	private final LoginService loginService;

	@GetMapping("/login")
	public String loginForm(
			@SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) LoginMember loginMember,
			@RequestParam(required = false) String redirectURL,
			Model model) {
		if (loginMember != null) {
			return "redirect:/";
		}
		LoginForm loginForm = new LoginForm();
		loginForm.setRedirectURL(redirectURL);
		model.addAttribute("loginForm", loginForm);
		return LOGIN_VIEW;
	}

	@PostMapping("/login")
	public String login(@Valid @ModelAttribute LoginForm loginForm, BindingResult bindingResult,
			HttpServletRequest request) {
		if (bindingResult.hasErrors()) {
			return LOGIN_VIEW;
		}

		Optional<LoginMember> loginMember = loginService.login(loginForm.getEmail(), loginForm.getPassword());
		if (loginMember.isEmpty()) {
			bindingResult.reject("loginFail", "이메일 또는 비밀번호가 올바르지 않습니다");
			return LOGIN_VIEW;
		}

		HttpSession session = request.getSession();
		request.changeSessionId(); // 세션 고정 공격 방지
		session.setAttribute(SessionConst.LOGIN_MEMBER, loginMember.get());
		return "redirect:" + RedirectUrls.safe(loginForm.getRedirectURL());
	}

	@PostMapping("/logout")
	public String logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		return "redirect:/";
	}
}

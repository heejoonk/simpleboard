package com.himedia.board.member.controller;

import com.himedia.board.global.session.LoginMember;
import com.himedia.board.global.session.SessionConst;
import com.himedia.board.member.dto.SignupRequest;
import com.himedia.board.member.service.DuplicateMemberException;
import com.himedia.board.member.service.MemberService;
import jakarta.validation.Valid;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
@RequiredArgsConstructor
public class MemberController {

	private static final String SIGNUP_VIEW = "member/signup";

	private final MemberService memberService;

	@GetMapping("/signup")
	public String signupForm(
			@SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) LoginMember loginMember,
			Model model) {
		if (loginMember != null) {
			return "redirect:/";
		}
		model.addAttribute("signupRequest", new SignupRequest());
		return SIGNUP_VIEW;
	}

	@PostMapping("/signup")
	public String signup(
			@SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) LoginMember loginMember,
			@Valid @ModelAttribute SignupRequest signupRequest,
			BindingResult bindingResult) {
		if (loginMember != null) {
			return "redirect:/";
		}
		if (!bindingResult.hasFieldErrors("passwordConfirm")
				&& !Objects.equals(signupRequest.getPassword(), signupRequest.getPasswordConfirm())) {
			bindingResult.rejectValue("passwordConfirm", "mismatch", "비밀번호가 일치하지 않습니다");
		}
		if (bindingResult.hasErrors()) {
			return SIGNUP_VIEW;
		}

		try {
			memberService.signup(signupRequest);
		} catch (DuplicateMemberException e) {
			e.getFieldErrors().forEach((field, message) -> bindingResult.rejectValue(field, "duplicate", message));
			return SIGNUP_VIEW;
		}
		return "redirect:/login?joined";
	}
}

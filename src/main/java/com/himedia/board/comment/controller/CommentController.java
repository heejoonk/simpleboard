package com.himedia.board.comment.controller;

import com.himedia.board.comment.dto.CommentForm;
import com.himedia.board.comment.service.CommentService;
import com.himedia.board.global.session.LoginMember;
import com.himedia.board.global.session.SessionConst;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class CommentController {

	static final String INVALID_COMMENT_MESSAGE = "댓글은 1~500자로 입력해주세요";

	private final CommentService commentService;

	@PostMapping("/posts/{postId}/comments")
	public String create(@PathVariable Long postId,
			@SessionAttribute(name = SessionConst.LOGIN_MEMBER) LoginMember loginMember,
			@Valid @ModelAttribute CommentForm commentForm, BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("errorMessage", INVALID_COMMENT_MESSAGE);
		} else {
			commentService.write(postId, loginMember, commentForm);
		}
		return "redirect:/posts/" + postId;
	}

	@PostMapping("/posts/{postId}/comments/{commentId}/delete")
	public String delete(@PathVariable Long postId, @PathVariable Long commentId,
			@SessionAttribute(name = SessionConst.LOGIN_MEMBER) LoginMember loginMember) {
		commentService.delete(postId, commentId, loginMember);
		return "redirect:/posts/" + postId;
	}
}

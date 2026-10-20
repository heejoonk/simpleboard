package com.himedia.board.post.controller;

import com.himedia.board.comment.dto.CommentForm;
import com.himedia.board.comment.service.CommentService;
import com.himedia.board.global.session.LoginMember;
import com.himedia.board.global.session.SessionConst;
import com.himedia.board.post.dto.PostForm;
import com.himedia.board.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
@RequiredArgsConstructor
public class PostController {

	private static final String FORM_VIEW = "post/form";

	private final PostService postService;
	private final CommentService commentService;

	@GetMapping({"/", "/posts"})
	public String list(@RequestParam(defaultValue = "0") int page, Model model) {
		model.addAttribute("page", postService.getPage(page));
		return "post/list";
	}

	@GetMapping("/posts/{id}")
	public String detail(@PathVariable Long id,
			@SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) LoginMember loginMember,
			Model model) {
		model.addAttribute("post", postService.getDetail(id, loginMember));
		model.addAttribute("comments", commentService.getComments(id, loginMember));
		model.addAttribute("commentForm", new CommentForm());
		return "post/detail";
	}

	@GetMapping("/posts/new")
	public String newForm(Model model) {
		model.addAttribute("postForm", new PostForm());
		return FORM_VIEW;
	}

	@PostMapping("/posts")
	public String create(@SessionAttribute(name = SessionConst.LOGIN_MEMBER) LoginMember loginMember,
			@Valid @ModelAttribute PostForm postForm, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return FORM_VIEW;
		}
		Long id = postService.write(loginMember, postForm);
		return "redirect:/posts/" + id;
	}

	@GetMapping("/posts/{id}/edit")
	public String editForm(@PathVariable Long id,
			@SessionAttribute(name = SessionConst.LOGIN_MEMBER) LoginMember loginMember,
			Model model) {
		model.addAttribute("postForm", postService.getEditForm(id, loginMember));
		model.addAttribute("postId", id);
		return FORM_VIEW;
	}

	@PostMapping("/posts/{id}/edit")
	public String edit(@PathVariable Long id,
			@SessionAttribute(name = SessionConst.LOGIN_MEMBER) LoginMember loginMember,
			@Valid @ModelAttribute PostForm postForm, BindingResult bindingResult,
			Model model) {
		if (bindingResult.hasErrors()) {
			postService.getEditForm(id, loginMember); // 폼을 다시 보여주기 전에 존재/권한 확인 (404, 403)
			model.addAttribute("postId", id);
			return FORM_VIEW;
		}
		postService.update(id, loginMember, postForm);
		return "redirect:/posts/" + id;
	}

	@PostMapping("/posts/{id}/delete")
	public String delete(@PathVariable Long id,
			@SessionAttribute(name = SessionConst.LOGIN_MEMBER) LoginMember loginMember) {
		postService.delete(id, loginMember);
		return "redirect:/posts";
	}
}

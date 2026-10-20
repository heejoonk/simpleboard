package com.himedia.board.post.service;

import com.himedia.board.global.common.PageResult;
import com.himedia.board.global.exception.ForbiddenException;
import com.himedia.board.global.exception.NotFoundException;
import com.himedia.board.global.session.LoginMember;
import com.himedia.board.post.domain.Post;
import com.himedia.board.post.dto.PostDetailResponse;
import com.himedia.board.post.dto.PostForm;
import com.himedia.board.post.dto.PostListResponse;
import com.himedia.board.post.repository.PostRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

	public static final int PAGE_SIZE = 10;

	private final PostRepository postRepository;

	@Transactional
	public Long write(LoginMember loginMember, PostForm form) {
		Post post = Post.builder()
				.memberId(loginMember.id())
				.title(form.getTitle())
				.content(form.getContent())
				.build();
		return postRepository.save(post);
	}

	/** 음수 페이지는 0으로, 범위를 넘는 페이지는 빈 목록으로 처리한다. 쿼리는 목록 1 + COUNT 1. */
	public PageResult<PostListResponse> getPage(int page) {
		int safePage = Math.max(page, 0);
		List<PostListResponse> content = postRepository.findPage((long) safePage * PAGE_SIZE, PAGE_SIZE);
		long total = postRepository.count();
		return PageResult.of(content, safePage, PAGE_SIZE, total);
	}

	/** 상세 조회. 조회수를 1 올린 뒤 조회한다. */
	@Transactional
	public PostDetailResponse getDetail(Long id, LoginMember loginMember) {
		if (postRepository.increaseViewCount(id) == 0) {
			throw notFound();
		}
		PostDetailResponse detail = postRepository.findDetailById(id).orElseThrow(this::notFound);
		return detail.withOwner(loginMember == null ? null : loginMember.id());
	}

	/** 수정 폼에 채울 기존 값. 작성자가 아니면 403. */
	public PostForm getEditForm(Long id, LoginMember loginMember) {
		Post post = getOwnedPost(id, loginMember);
		return new PostForm(post.getTitle(), post.getContent());
	}

	@Transactional
	public void update(Long id, LoginMember loginMember, PostForm form) {
		getOwnedPost(id, loginMember);
		if (postRepository.update(id, form.getTitle(), form.getContent()) == 0) {
			throw notFound();
		}
	}

	@Transactional
	public void delete(Long id, LoginMember loginMember) {
		getOwnedPost(id, loginMember);
		if (postRepository.deleteById(id) == 0) {
			throw notFound();
		}
	}

	private Post getOwnedPost(Long id, LoginMember loginMember) {
		Post post = postRepository.findById(id).orElseThrow(this::notFound);
		if (!post.getMemberId().equals(loginMember.id())) {
			throw new ForbiddenException("게시글 작성자가 아닙니다. postId=" + id);
		}
		return post;
	}

	private NotFoundException notFound() {
		return new NotFoundException("게시글이 없습니다.");
	}
}

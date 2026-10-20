package com.himedia.board.comment.service;

import com.himedia.board.comment.domain.Comment;
import com.himedia.board.comment.dto.CommentForm;
import com.himedia.board.comment.dto.CommentResponse;
import com.himedia.board.comment.repository.CommentRepository;
import com.himedia.board.global.exception.ForbiddenException;
import com.himedia.board.global.exception.NotFoundException;
import com.himedia.board.global.session.LoginMember;
import com.himedia.board.post.repository.PostRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

	private final CommentRepository commentRepository;
	private final PostRepository postRepository;

	public List<CommentResponse> getComments(Long postId, LoginMember loginMember) {
		Long loginMemberId = loginMember == null ? null : loginMember.id();
		return commentRepository.findResponsesByPostId(postId).stream()
				.map(comment -> comment.withOwner(loginMemberId))
				.toList();
	}

	@Transactional
	public Long write(Long postId, LoginMember loginMember, CommentForm form) {
		if (!postRepository.existsById(postId)) {
			throw new NotFoundException("게시글이 없습니다.");
		}
		Comment comment = Comment.builder()
				.postId(postId)
				.memberId(loginMember.id())
				.content(form.getContent())
				.build();
		return commentRepository.save(comment);
	}

	/** 댓글이 없거나 URL의 글(postId)에 속하지 않으면 404, 작성자가 아니면 403 */
	@Transactional
	public void delete(Long postId, Long commentId, LoginMember loginMember) {
		Comment comment = commentRepository.findById(commentId)
				.filter(c -> c.getPostId().equals(postId))
				.orElseThrow(() -> new NotFoundException("댓글이 없습니다."));
		if (!comment.getMemberId().equals(loginMember.id())) {
			throw new ForbiddenException("댓글 작성자가 아닙니다. commentId=" + commentId);
		}
		commentRepository.deleteById(commentId);
	}
}

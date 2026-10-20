package com.himedia.board.comment.dto;

import java.time.LocalDateTime;

public record CommentResponse(
		Long id,
		String content,
		Long writerId,
		String writerNickname,
		LocalDateTime createdAt,
		boolean owner) {

	/** 로그인 사용자가 작성자인지 반영한 사본을 돌려준다. (비로그인이면 loginMemberId = null) */
	public CommentResponse withOwner(Long loginMemberId) {
		boolean isOwner = loginMemberId != null && loginMemberId.equals(writerId);
		return new CommentResponse(id, content, writerId, writerNickname, createdAt, isOwner);
	}
}

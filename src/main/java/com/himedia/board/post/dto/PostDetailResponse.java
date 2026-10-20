package com.himedia.board.post.dto;

import java.time.LocalDateTime;

public record PostDetailResponse(
		Long id,
		String title,
		String content,
		Long writerId,
		String writerNickname,
		int viewCount,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		boolean owner,
		boolean edited) {

	/** 로그인 사용자가 작성자인지 반영한 사본을 돌려준다. (비로그인이면 loginMemberId = null) */
	public PostDetailResponse withOwner(Long loginMemberId) {
		boolean isOwner = loginMemberId != null && loginMemberId.equals(writerId);
		return new PostDetailResponse(id, title, content, writerId, writerNickname, viewCount,
				createdAt, updatedAt, isOwner, edited);
	}
}

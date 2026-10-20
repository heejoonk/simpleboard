package com.himedia.board.post.dto;

import java.time.LocalDateTime;

public record PostListResponse(
		Long id,
		String title,
		String writerNickname,
		int commentCount,
		int viewCount,
		LocalDateTime createdAt) {
}

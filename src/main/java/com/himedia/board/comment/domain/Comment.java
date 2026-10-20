package com.himedia.board.comment.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Comment {

	private Long id;
	private Long postId;
	private Long memberId;
	private String content;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}

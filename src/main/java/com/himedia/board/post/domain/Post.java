package com.himedia.board.post.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Post {

	private Long id;
	private Long memberId;
	private String title;
	private String content;
	private int viewCount;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}

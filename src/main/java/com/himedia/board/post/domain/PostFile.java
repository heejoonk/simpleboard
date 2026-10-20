package com.himedia.board.post.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/** 게시글 첨부 파일 (글당 1개). 실제 파일은 디스크에 storedName 으로 저장된다. */
@Getter
@Builder
public class PostFile {

	private Long id;
	private Long postId;
	private String originalName;
	private String storedName;
	private long fileSize;
	private LocalDateTime createdAt;
}

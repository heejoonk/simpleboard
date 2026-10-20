package com.himedia.board.post.dto;

import com.himedia.board.post.domain.AttachmentPolicy;
import com.himedia.board.post.domain.PostFile;

/** 상세/수정 화면에 보여줄 첨부 파일 정보. 저장 파일명(storedName)은 화면에 내보내지 않는다. */
public record PostFileResponse(Long id, Long postId, String originalName, long fileSize, boolean image) {

	public static PostFileResponse from(PostFile file) {
		return new PostFileResponse(file.getId(), file.getPostId(), file.getOriginalName(), file.getFileSize(),
				AttachmentPolicy.isImage(file.getOriginalName()));
	}

	/** 예: 512 B, 3.4 KB, 1.2 MB */
	public String displaySize() {
		if (fileSize < 1024) {
			return fileSize + " B";
		}
		if (fileSize < 1024 * 1024) {
			return String.format("%.1f KB", fileSize / 1024.0);
		}
		return String.format("%.1f MB", fileSize / (1024.0 * 1024));
	}
}

package com.himedia.board.post.controller;

import com.himedia.board.post.domain.AttachmentPolicy;
import com.himedia.board.post.dto.FileDownload;
import com.himedia.board.post.service.PostService;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** 첨부 파일 다운로드 / 이미지 미리보기 (F-10). 로그인 체크는 LoginCheckInterceptor 가 한다. */
@Controller
@RequiredArgsConstructor
public class PostFileController {

	private final PostService postService;

	@GetMapping("/posts/{postId}/files/{fileId}")
	public ResponseEntity<Resource> download(@PathVariable Long postId, @PathVariable Long fileId) {
		FileDownload file = postService.getDownload(postId, fileId);
		return response(file, MediaType.APPLICATION_OCTET_STREAM,
				ContentDisposition.attachment().filename(file.originalName(), StandardCharsets.UTF_8).build());
	}

	@GetMapping("/posts/{postId}/files/{fileId}/preview")
	public ResponseEntity<Resource> preview(@PathVariable Long postId, @PathVariable Long fileId) {
		FileDownload file = postService.getPreview(postId, fileId);
		MediaType imageType = AttachmentPolicy.imageMediaType(file.originalName()).orElseThrow();
		return response(file, imageType,
				ContentDisposition.inline().filename(file.originalName(), StandardCharsets.UTF_8).build());
	}

	/** 브라우저가 내용을 보고 형식을 추측하지 않도록 nosniff 를 붙인다. */
	private ResponseEntity<Resource> response(FileDownload file, MediaType type, ContentDisposition disposition) {
		return ResponseEntity.ok()
				.contentType(type)
				.contentLength(file.fileSize())
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
				.header("X-Content-Type-Options", "nosniff")
				.body(file.resource());
	}
}

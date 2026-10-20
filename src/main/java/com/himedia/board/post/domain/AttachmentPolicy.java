package com.himedia.board.post.domain;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.MediaType;

/**
 * 첨부 파일 규칙 (F-10). 파일 형식은 클라이언트가 보낸 Content-Type 이 아니라 확장자로만 판단한다.
 * SVG 는 스크립트를 담을 수 있으므로 허용하지 않는다.
 */
public final class AttachmentPolicy {

	public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
	public static final int MAX_NAME_LENGTH = 255;

	private static final Map<String, MediaType> IMAGE_TYPES = Map.of(
			"jpg", MediaType.IMAGE_JPEG,
			"jpeg", MediaType.IMAGE_JPEG,
			"png", MediaType.IMAGE_PNG,
			"gif", MediaType.IMAGE_GIF,
			"webp", MediaType.parseMediaType("image/webp"));

	private static final List<String> DOCUMENT_EXTENSIONS = List.of(
			"pdf", "txt", "zip", "hwp", "doc", "docx", "xls", "xlsx", "ppt", "pptx");

	public static final String ALLOWED_EXTENSIONS_TEXT =
			"jpg, jpeg, png, gif, webp, " + String.join(", ", DOCUMENT_EXTENSIONS);

	private AttachmentPolicy() {
	}

	/** 소문자 확장자. 없으면 빈 문자열. */
	public static String extensionOf(String filename) {
		if (filename == null) {
			return "";
		}
		int dot = filename.lastIndexOf('.');
		return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
	}

	public static boolean isAllowed(String filename) {
		String ext = extensionOf(filename);
		return IMAGE_TYPES.containsKey(ext) || DOCUMENT_EXTENSIONS.contains(ext);
	}

	public static boolean isImage(String filename) {
		return IMAGE_TYPES.containsKey(extensionOf(filename));
	}

	public static Optional<MediaType> imageMediaType(String filename) {
		return Optional.ofNullable(IMAGE_TYPES.get(extensionOf(filename)));
	}
}

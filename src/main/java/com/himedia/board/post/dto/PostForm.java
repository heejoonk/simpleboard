package com.himedia.board.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class PostForm {

	@NotBlank(message = "제목을 입력해주세요")
	@Size(max = 100, message = "제목은 100자 이하로 입력해주세요")
	private String title;

	@NotBlank(message = "본문을 입력해주세요")
	@Size(max = 5000, message = "본문은 5000자 이하로 입력해주세요")
	private String content;

	/** 새 첨부 파일 (선택). 수정 시 기존 파일이 있으면 교체된다. (F-10) */
	@AllowedAttachment
	private MultipartFile file;

	/** 수정 화면의 "기존 파일 삭제" 체크 여부 (F-10) */
	private boolean deleteFile;

	public PostForm(String title, String content) {
		this.title = title;
		this.content = content;
	}

	public boolean hasFile() {
		return file != null && !file.isEmpty();
	}

	/** 일부 브라우저가 보내는 경로(C:\...\a.png)를 떼고 파일명만 남긴다. */
	public static String cleanFilename(String originalFilename) {
		if (originalFilename == null) {
			return "";
		}
		String name = StringUtils.getFilename(originalFilename.replace('\\', '/'));
		return name == null ? "" : name.strip();
	}
}

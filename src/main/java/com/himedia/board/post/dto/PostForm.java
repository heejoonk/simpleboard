package com.himedia.board.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostForm {

	@NotBlank(message = "제목을 입력해주세요")
	@Size(max = 100, message = "제목은 100자 이하로 입력해주세요")
	private String title;

	@NotBlank(message = "본문을 입력해주세요")
	@Size(max = 5000, message = "본문은 5000자 이하로 입력해주세요")
	private String content;
}

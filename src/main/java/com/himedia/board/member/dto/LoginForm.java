package com.himedia.board.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginForm {

	@NotBlank(message = "이메일을 입력해주세요")
	private String email;

	@NotBlank(message = "비밀번호를 입력해주세요")
	private String password;

	/** 숨은 필드. "/"로 시작하고 "//"로 시작하지 않을 때만 사용한다. */
	private String redirectURL;
}

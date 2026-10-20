package com.himedia.board.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SignupRequest {

	@NotBlank(message = "이메일을 입력해주세요")
	@Email(message = "이메일 형식이 올바르지 않습니다")
	@Size(max = 100, message = "이메일은 100자 이하로 입력해주세요")
	private String email;

	// 빈 값은 @NotBlank 메시지만 나오도록 패턴에서 빈 문자열은 허용한다
	@NotBlank(message = "비밀번호를 입력해주세요")
	@Pattern(regexp = "^$|^(?=.*[A-Za-z])(?=.*\\d).{8,20}$",
			message = "비밀번호는 영문과 숫자를 포함해 8~20자로 입력해주세요")
	private String password;

	@NotBlank(message = "비밀번호 확인을 입력해주세요")
	private String passwordConfirm;

	@NotBlank(message = "닉네임을 입력해주세요")
	@Pattern(regexp = "^$|^.{2,10}$", message = "닉네임은 2~10자로 입력해주세요")
	private String nickname;
}

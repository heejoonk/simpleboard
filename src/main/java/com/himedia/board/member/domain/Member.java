package com.himedia.board.member.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Member {

	private Long id;
	private String email;
	private String password;
	private String nickname;
	private String role;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}

package com.himedia.board.member.service;

import java.util.Map;
import lombok.Getter;

/**
 * 회원가입 시 이메일/닉네임 중복. fieldErrors는 필드명 → 화면 메시지.
 */
@Getter
public class DuplicateMemberException extends RuntimeException {

	private final Map<String, String> fieldErrors;

	public DuplicateMemberException(Map<String, String> fieldErrors) {
		super("중복된 회원 정보: " + fieldErrors.keySet());
		this.fieldErrors = Map.copyOf(fieldErrors);
	}
}

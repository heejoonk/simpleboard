package com.himedia.board.global.session;

import java.io.Serializable;

/**
 * 세션에 저장하는 로그인 사용자 정보. 비밀번호는 절대 포함하지 않는다.
 */
public record LoginMember(Long id, String email, String nickname) implements Serializable {
}

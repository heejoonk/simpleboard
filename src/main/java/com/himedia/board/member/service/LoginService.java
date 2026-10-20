package com.himedia.board.member.service;

import com.himedia.board.global.session.LoginMember;
import com.himedia.board.member.repository.MemberRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	/**
	 * 이메일과 비밀번호가 맞으면 세션에 저장할 LoginMember(비밀번호 제외)를 돌려준다.
	 * 어느 쪽이 틀렸는지는 구분하지 않는다.
	 */
	public Optional<LoginMember> login(String email, String password) {
		return memberRepository.findByEmail(email)
				.filter(member -> passwordEncoder.matches(password, member.getPassword()))
				.map(member -> new LoginMember(member.getId(), member.getEmail(), member.getNickname()));
	}
}

package com.himedia.board.member.service;

import com.himedia.board.member.domain.Member;
import com.himedia.board.member.dto.SignupRequest;
import com.himedia.board.member.repository.MemberRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

	static final String DUPLICATE_EMAIL_MESSAGE = "이미 사용 중인 이메일입니다";
	static final String DUPLICATE_NICKNAME_MESSAGE = "이미 사용 중인 닉네임입니다";

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public Long signup(SignupRequest request) {
		Map<String, String> duplicates = new LinkedHashMap<>();
		if (memberRepository.existsByEmail(request.getEmail())) {
			duplicates.put("email", DUPLICATE_EMAIL_MESSAGE);
		}
		if (memberRepository.existsByNickname(request.getNickname())) {
			duplicates.put("nickname", DUPLICATE_NICKNAME_MESSAGE);
		}
		if (!duplicates.isEmpty()) {
			throw new DuplicateMemberException(duplicates);
		}

		Member member = Member.builder()
				.email(request.getEmail())
				.password(passwordEncoder.encode(request.getPassword()))
				.nickname(request.getNickname())
				.build();
		try {
			return memberRepository.save(member);
		} catch (DuplicateKeyException e) {
			// SELECT 확인 이후 동시에 가입한 경우: UNIQUE 제약 위반을 같은 메시지로 처리
			throw toDuplicateMemberException(e);
		}
	}

	private DuplicateMemberException toDuplicateMemberException(DuplicateKeyException e) {
		String message = String.valueOf(e.getMessage());
		if (message.contains("uk_member_nickname")) {
			return new DuplicateMemberException(Map.of("nickname", DUPLICATE_NICKNAME_MESSAGE));
		}
		return new DuplicateMemberException(Map.of("email", DUPLICATE_EMAIL_MESSAGE));
	}
}

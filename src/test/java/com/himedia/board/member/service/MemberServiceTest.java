package com.himedia.board.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.himedia.board.global.session.LoginMember;
import com.himedia.board.member.domain.Member;
import com.himedia.board.member.dto.SignupRequest;
import com.himedia.board.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MemberServiceTest {

	@Autowired
	MemberService memberService;
	@Autowired
	LoginService loginService;
	@Autowired
	MemberRepository memberRepository;
	@Autowired
	PasswordEncoder passwordEncoder;

	private SignupRequest request(String email, String nickname) {
		SignupRequest request = new SignupRequest();
		request.setEmail(email);
		request.setPassword("password1");
		request.setPasswordConfirm("password1");
		request.setNickname(nickname);
		return request;
	}

	@Test
	void F01_정상_가입하면_BCrypt로_암호화된_비밀번호가_저장된다() {
		Long id = memberService.signup(request("svc-a@test.com", "서비스A"));

		Member saved = memberRepository.findById(id).orElseThrow();
		assertThat(saved.getPassword()).isNotEqualTo("password1").startsWith("$2");
		assertThat(passwordEncoder.matches("password1", saved.getPassword())).isTrue();
	}

	@Test
	void F01_이메일이_중복되면_가입되지_않는다() {
		memberService.signup(request("svc-b@test.com", "서비스B"));

		assertThatThrownBy(() -> memberService.signup(request("svc-b@test.com", "서비스B2")))
				.isInstanceOfSatisfying(DuplicateMemberException.class, e -> assertThat(e.getFieldErrors())
						.containsEntry("email", "이미 사용 중인 이메일입니다")
						.doesNotContainKey("nickname"));
	}

	@Test
	void F01_닉네임이_중복되면_가입되지_않는다() {
		memberService.signup(request("svc-c@test.com", "서비스C"));

		assertThatThrownBy(() -> memberService.signup(request("svc-c2@test.com", "서비스C")))
				.isInstanceOfSatisfying(DuplicateMemberException.class, e -> assertThat(e.getFieldErrors())
						.containsEntry("nickname", "이미 사용 중인 닉네임입니다")
						.doesNotContainKey("email"));
	}

	@Test
	void F02_이메일과_비밀번호가_맞으면_비밀번호_없는_LoginMember를_돌려준다() {
		Long id = memberService.signup(request("svc-d@test.com", "서비스D"));

		LoginMember loginMember = loginService.login("svc-d@test.com", "password1").orElseThrow();

		assertThat(loginMember).isEqualTo(new LoginMember(id, "svc-d@test.com", "서비스D"));
	}

	@Test
	void F02_비밀번호가_틀리거나_없는_이메일이면_로그인에_실패한다() {
		memberService.signup(request("svc-e@test.com", "서비스E"));

		assertThat(loginService.login("svc-e@test.com", "wrongpass1")).isEmpty();
		assertThat(loginService.login("nobody@test.com", "password1")).isEmpty();
	}
}

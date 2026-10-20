package com.himedia.board.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.himedia.board.member.domain.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MemberRepository.class)
class MemberRepositoryTest {

	@Autowired
	MemberRepository memberRepository;

	private Member member(String email, String nickname) {
		return Member.builder().email(email).password("hashed").nickname(nickname).build();
	}

	@Test
	void F01_회원을_저장하고_이메일로_조회한다() {
		Long id = memberRepository.save(member("repo-a@test.com", "레포A"));

		Member found = memberRepository.findByEmail("repo-a@test.com").orElseThrow();
		assertThat(found.getId()).isEqualTo(id);
		assertThat(found.getNickname()).isEqualTo("레포A");
		assertThat(found.getRole()).isEqualTo("USER");
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(memberRepository.findById(id)).isPresent();
	}

	@Test
	void F01_이메일과_닉네임_존재여부를_확인한다() {
		memberRepository.save(member("repo-b@test.com", "레포B"));

		assertThat(memberRepository.existsByEmail("repo-b@test.com")).isTrue();
		assertThat(memberRepository.existsByEmail("none@test.com")).isFalse();
		assertThat(memberRepository.existsByNickname("레포B")).isTrue();
		assertThat(memberRepository.existsByNickname("없는닉")).isFalse();
	}

	@Test
	void F01_중복_이메일은_UNIQUE_제약으로_저장되지_않는다() {
		memberRepository.save(member("repo-c@test.com", "레포C"));

		assertThatThrownBy(() -> memberRepository.save(member("repo-c@test.com", "레포C2")))
				.isInstanceOf(DuplicateKeyException.class);
	}

	@Test
	void F02_없는_이메일은_빈_Optional() {
		assertThat(memberRepository.findByEmail("nobody@test.com")).isEmpty();
	}
}

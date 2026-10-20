package com.himedia.board.member.repository;

import com.himedia.board.member.domain.Member;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberRepository {

	private static final RowMapper<Member> MEMBER_ROW_MAPPER = (rs, rowNum) -> Member.builder()
			.id(rs.getLong("id"))
			.email(rs.getString("email"))
			.password(rs.getString("password"))
			.nickname(rs.getString("nickname"))
			.role(rs.getString("role"))
			.createdAt(rs.getObject("created_at", LocalDateTime.class))
			.updatedAt(rs.getObject("updated_at", LocalDateTime.class))
			.build();

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public Long save(Member member) {
		String sql = """
				INSERT INTO member (email, password, nickname)
				VALUES (:email, :password, :nickname)
				""";
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("email", member.getEmail())
				.addValue("password", member.getPassword())
				.addValue("nickname", member.getNickname());
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(sql, params, keyHolder, new String[] {"id"});
		return Objects.requireNonNull(keyHolder.getKey()).longValue();
	}

	public Optional<Member> findById(Long id) {
		String sql = """
				SELECT id, email, password, nickname, role, created_at, updated_at
				FROM member
				WHERE id = :id
				""";
		return jdbcTemplate.query(sql, new MapSqlParameterSource("id", id), MEMBER_ROW_MAPPER)
				.stream().findFirst();
	}

	public Optional<Member> findByEmail(String email) {
		String sql = """
				SELECT id, email, password, nickname, role, created_at, updated_at
				FROM member
				WHERE email = :email
				""";
		return jdbcTemplate.query(sql, new MapSqlParameterSource("email", email), MEMBER_ROW_MAPPER)
				.stream().findFirst();
	}

	public boolean existsByEmail(String email) {
		String sql = """
				SELECT EXISTS (SELECT 1 FROM member WHERE email = :email)
				""";
		return Boolean.TRUE.equals(
				jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("email", email), Boolean.class));
	}

	public boolean existsByNickname(String nickname) {
		String sql = """
				SELECT EXISTS (SELECT 1 FROM member WHERE nickname = :nickname)
				""";
		return Boolean.TRUE.equals(
				jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("nickname", nickname), Boolean.class));
	}
}

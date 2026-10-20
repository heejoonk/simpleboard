package com.himedia.board.comment.repository;

import com.himedia.board.comment.domain.Comment;
import com.himedia.board.comment.dto.CommentResponse;
import java.time.LocalDateTime;
import java.util.List;
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
public class CommentRepository {

	private static final RowMapper<Comment> COMMENT_ROW_MAPPER = (rs, rowNum) -> Comment.builder()
			.id(rs.getLong("id"))
			.postId(rs.getLong("post_id"))
			.memberId(rs.getLong("member_id"))
			.content(rs.getString("content"))
			.createdAt(rs.getObject("created_at", LocalDateTime.class))
			.updatedAt(rs.getObject("updated_at", LocalDateTime.class))
			.build();

	private static final RowMapper<CommentResponse> RESPONSE_ROW_MAPPER = (rs, rowNum) -> new CommentResponse(
			rs.getLong("id"),
			rs.getString("content"),
			rs.getLong("member_id"),
			rs.getString("writer_nickname"),
			rs.getObject("created_at", LocalDateTime.class),
			false);

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public Long save(Comment comment) {
		String sql = """
				INSERT INTO comment (post_id, member_id, content)
				VALUES (:postId, :memberId, :content)
				""";
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("postId", comment.getPostId())
				.addValue("memberId", comment.getMemberId())
				.addValue("content", comment.getContent());
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(sql, params, keyHolder, new String[] {"id"});
		return Objects.requireNonNull(keyHolder.getKey()).longValue();
	}

	public Optional<Comment> findById(Long id) {
		String sql = """
				SELECT id, post_id, member_id, content, created_at, updated_at
				FROM comment
				WHERE id = :id
				""";
		return jdbcTemplate.query(sql, new MapSqlParameterSource("id", id), COMMENT_ROW_MAPPER)
				.stream().findFirst();
	}

	/** 상세 화면 댓글 목록: 작성순(오래된 순), 작성자 닉네임 JOIN */
	public List<CommentResponse> findResponsesByPostId(Long postId) {
		String sql = """
				SELECT c.id, c.content, c.member_id, c.created_at, m.nickname AS writer_nickname
				FROM comment c
				JOIN member m ON c.member_id = m.id
				WHERE c.post_id = :postId
				ORDER BY c.id ASC
				""";
		return jdbcTemplate.query(sql, new MapSqlParameterSource("postId", postId), RESPONSE_ROW_MAPPER);
	}

	public int deleteById(Long id) {
		String sql = """
				DELETE FROM comment WHERE id = :id
				""";
		return jdbcTemplate.update(sql, new MapSqlParameterSource("id", id));
	}
}

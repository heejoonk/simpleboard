package com.himedia.board.post.repository;

import com.himedia.board.post.domain.Post;
import com.himedia.board.post.dto.PostDetailResponse;
import com.himedia.board.post.dto.PostListResponse;
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
public class PostRepository {

	private static final RowMapper<Post> POST_ROW_MAPPER = (rs, rowNum) -> Post.builder()
			.id(rs.getLong("id"))
			.memberId(rs.getLong("member_id"))
			.title(rs.getString("title"))
			.content(rs.getString("content"))
			.viewCount(rs.getInt("view_count"))
			.createdAt(rs.getObject("created_at", LocalDateTime.class))
			.updatedAt(rs.getObject("updated_at", LocalDateTime.class))
			.build();

	private static final RowMapper<PostListResponse> LIST_ROW_MAPPER = (rs, rowNum) -> new PostListResponse(
			rs.getLong("id"),
			rs.getString("title"),
			rs.getString("writer_nickname"),
			rs.getInt("comment_count"),
			rs.getInt("view_count"),
			rs.getObject("created_at", LocalDateTime.class));

	private static final RowMapper<PostDetailResponse> DETAIL_ROW_MAPPER = (rs, rowNum) -> {
		LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);
		LocalDateTime updatedAt = rs.getObject("updated_at", LocalDateTime.class);
		return new PostDetailResponse(
				rs.getLong("id"),
				rs.getString("title"),
				rs.getString("content"),
				rs.getLong("member_id"),
				rs.getString("writer_nickname"),
				rs.getInt("view_count"),
				createdAt,
				updatedAt,
				false,
				updatedAt.isAfter(createdAt));
	};

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public Long save(Post post) {
		String sql = """
				INSERT INTO post (member_id, title, content)
				VALUES (:memberId, :title, :content)
				""";
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("memberId", post.getMemberId())
				.addValue("title", post.getTitle())
				.addValue("content", post.getContent());
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(sql, params, keyHolder, new String[] {"id"});
		return Objects.requireNonNull(keyHolder.getKey()).longValue();
	}

	public Optional<Post> findById(Long id) {
		String sql = """
				SELECT id, member_id, title, content, view_count, created_at, updated_at
				FROM post
				WHERE id = :id
				""";
		return jdbcTemplate.query(sql, new MapSqlParameterSource("id", id), POST_ROW_MAPPER)
				.stream().findFirst();
	}

	public boolean existsById(Long id) {
		String sql = """
				SELECT EXISTS (SELECT 1 FROM post WHERE id = :id)
				""";
		return Boolean.TRUE.equals(
				jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("id", id), Boolean.class));
	}

	/** 목록: post + member JOIN, 댓글 수는 서브쿼리로 한 번에 가져온다 (N+1 없음) */
	public List<PostListResponse> findPage(long offset, int size) {
		String sql = """
				SELECT p.id, p.title, p.view_count, p.created_at,
				       m.nickname AS writer_nickname,
				       (SELECT COUNT(*) FROM comment c WHERE c.post_id = p.id) AS comment_count
				FROM post p
				JOIN member m ON p.member_id = m.id
				ORDER BY p.id DESC
				LIMIT :size OFFSET :offset
				""";
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("size", size)
				.addValue("offset", offset);
		return jdbcTemplate.query(sql, params, LIST_ROW_MAPPER);
	}

	public long count() {
		String sql = """
				SELECT COUNT(*) FROM post
				""";
		Long count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Long.class);
		return count == null ? 0 : count;
	}

	public Optional<PostDetailResponse> findDetailById(Long id) {
		String sql = """
				SELECT p.id, p.member_id, p.title, p.content, p.view_count, p.created_at, p.updated_at,
				       m.nickname AS writer_nickname
				FROM post p
				JOIN member m ON p.member_id = m.id
				WHERE p.id = :id
				""";
		return jdbcTemplate.query(sql, new MapSqlParameterSource("id", id), DETAIL_ROW_MAPPER)
				.stream().findFirst();
	}

	/** 조회수 증가. updated_at 이 바뀌지 않도록 updated_at = updated_at 를 함께 지정한다. */
	public int increaseViewCount(Long id) {
		String sql = """
				UPDATE post
				SET view_count = view_count + 1, updated_at = updated_at
				WHERE id = :id
				""";
		return jdbcTemplate.update(sql, new MapSqlParameterSource("id", id));
	}

	/** 내용이 같아도 수정일이 갱신되도록 updated_at 을 명시적으로 지정한다. */
	public int update(Long id, String title, String content) {
		String sql = """
				UPDATE post
				SET title = :title, content = :content, updated_at = CURRENT_TIMESTAMP
				WHERE id = :id
				""";
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("id", id)
				.addValue("title", title)
				.addValue("content", content);
		return jdbcTemplate.update(sql, params);
	}

	/** 댓글은 FK ON DELETE CASCADE 로 함께 삭제된다. */
	public int deleteById(Long id) {
		String sql = """
				DELETE FROM post WHERE id = :id
				""";
		return jdbcTemplate.update(sql, new MapSqlParameterSource("id", id));
	}
}

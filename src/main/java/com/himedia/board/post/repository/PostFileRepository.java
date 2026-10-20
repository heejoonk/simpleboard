package com.himedia.board.post.repository;

import com.himedia.board.post.domain.PostFile;
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
public class PostFileRepository {

	private static final RowMapper<PostFile> ROW_MAPPER = (rs, rowNum) -> PostFile.builder()
			.id(rs.getLong("id"))
			.postId(rs.getLong("post_id"))
			.originalName(rs.getString("original_name"))
			.storedName(rs.getString("stored_name"))
			.fileSize(rs.getLong("file_size"))
			.createdAt(rs.getObject("created_at", LocalDateTime.class))
			.build();

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public Long save(PostFile postFile) {
		String sql = """
				INSERT INTO post_file (post_id, original_name, stored_name, file_size)
				VALUES (:postId, :originalName, :storedName, :fileSize)
				""";
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("postId", postFile.getPostId())
				.addValue("originalName", postFile.getOriginalName())
				.addValue("storedName", postFile.getStoredName())
				.addValue("fileSize", postFile.getFileSize());
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(sql, params, keyHolder, new String[] {"id"});
		return Objects.requireNonNull(keyHolder.getKey()).longValue();
	}

	public Optional<PostFile> findByPostId(Long postId) {
		String sql = """
				SELECT id, post_id, original_name, stored_name, file_size, created_at
				FROM post_file
				WHERE post_id = :postId
				""";
		return jdbcTemplate.query(sql, new MapSqlParameterSource("postId", postId), ROW_MAPPER)
				.stream().findFirst();
	}

	/** 파일이 URL의 글(postId)에 속할 때만 조회된다. */
	public Optional<PostFile> findByIdAndPostId(Long id, Long postId) {
		String sql = """
				SELECT id, post_id, original_name, stored_name, file_size, created_at
				FROM post_file
				WHERE id = :id AND post_id = :postId
				""";
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("id", id)
				.addValue("postId", postId);
		return jdbcTemplate.query(sql, params, ROW_MAPPER).stream().findFirst();
	}

	public int deleteById(Long id) {
		String sql = """
				DELETE FROM post_file WHERE id = :id
				""";
		return jdbcTemplate.update(sql, new MapSqlParameterSource("id", id));
	}
}

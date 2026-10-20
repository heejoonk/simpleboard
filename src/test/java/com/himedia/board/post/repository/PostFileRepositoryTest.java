package com.himedia.board.post.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.himedia.board.member.domain.Member;
import com.himedia.board.member.repository.MemberRepository;
import com.himedia.board.post.domain.Post;
import com.himedia.board.post.domain.PostFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostFileRepository.class, PostRepository.class, MemberRepository.class})
class PostFileRepositoryTest {

	@Autowired
	PostFileRepository postFileRepository;
	@Autowired
	PostRepository postRepository;
	@Autowired
	MemberRepository memberRepository;

	Long postId;

	@BeforeEach
	void setUp() {
		Long memberId = memberRepository.save(
				Member.builder().email("file-repo@test.com").password("hashed").nickname("파일작성자").build());
		postId = postRepository.save(Post.builder().memberId(memberId).title("제목").content("본문").build());
	}

	private Long saveFile(Long postId, String storedName) {
		return postFileRepository.save(PostFile.builder()
				.postId(postId).originalName("보고서.pdf").storedName(storedName).fileSize(1234).build());
	}

	@Test
	void F10_첨부_파일을_저장하면_글_id로_조회된다() {
		Long id = saveFile(postId, "repo-test-1.pdf");

		PostFile file = postFileRepository.findByPostId(postId).orElseThrow();
		assertThat(file.getId()).isEqualTo(id);
		assertThat(file.getOriginalName()).isEqualTo("보고서.pdf");
		assertThat(file.getStoredName()).isEqualTo("repo-test-1.pdf");
		assertThat(file.getFileSize()).isEqualTo(1234);
		assertThat(file.getCreatedAt()).isNotNull();
	}

	@Test
	void F10_파일이_다른_글에_속하면_조회되지_않는다() {
		Long id = saveFile(postId, "repo-test-2.pdf");

		assertThat(postFileRepository.findByIdAndPostId(id, postId)).isPresent();
		assertThat(postFileRepository.findByIdAndPostId(id, postId + 1)).isEmpty();
	}

	@Test
	void F10_글당_첨부_파일은_1개만_저장된다() {
		saveFile(postId, "repo-test-3.pdf");

		assertThatThrownBy(() -> saveFile(postId, "repo-test-4.pdf")).isInstanceOf(DuplicateKeyException.class);
	}

	@Test
	void F10_첨부_파일을_삭제할_수_있고_글을_삭제하면_함께_삭제된다() {
		Long id = saveFile(postId, "repo-test-5.pdf");
		assertThat(postFileRepository.deleteById(id)).isEqualTo(1);
		assertThat(postFileRepository.findByPostId(postId)).isEmpty();

		saveFile(postId, "repo-test-6.pdf");
		postRepository.deleteById(postId);
		assertThat(postFileRepository.findByPostId(postId)).isEmpty();
	}
}

package com.himedia.board.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.himedia.board.global.exception.ForbiddenException;
import com.himedia.board.global.exception.NotFoundException;
import com.himedia.board.global.session.LoginMember;
import com.himedia.board.member.domain.Member;
import com.himedia.board.member.repository.MemberRepository;
import com.himedia.board.post.domain.PostFile;
import com.himedia.board.post.dto.FileDownload;
import com.himedia.board.post.dto.PostFileResponse;
import com.himedia.board.post.dto.PostForm;
import com.himedia.board.post.repository.PostFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

/** F-10 첨부 파일. 테스트가 롤백되면 FileStore 가 저장한 디스크 파일도 지운다. */
@SpringBootTest
@Transactional
class PostAttachmentServiceTest {

	@Autowired
	PostService postService;
	@Autowired
	PostFileRepository postFileRepository;
	@Autowired
	MemberRepository memberRepository;

	LoginMember writer;
	LoginMember other;

	@BeforeEach
	void setUp() {
		writer = saveMember("file-svc-w@test.com", "파일주인");
		other = saveMember("file-svc-o@test.com", "남의파일");
	}

	private LoginMember saveMember(String email, String nickname) {
		Long id = memberRepository.save(
				Member.builder().email(email).password("hashed").nickname(nickname).build());
		return new LoginMember(id, email, nickname);
	}

	private PostForm formWithFile(String filename) {
		PostForm form = new PostForm("제목", "본문");
		form.setFile(new MockMultipartFile("file", filename, "application/octet-stream", "data".getBytes()));
		return form;
	}

	@Test
	void F10_첨부_파일과_함께_글을_작성하면_파일_정보가_저장된다() throws Exception {
		Long postId = postService.write(writer, formWithFile("C:\\Users\\me\\사진.png"));

		PostFileResponse attachment = postService.getAttachment(postId).orElseThrow();
		assertThat(attachment.originalName()).isEqualTo("사진.png");
		assertThat(attachment.fileSize()).isEqualTo(4);
		assertThat(attachment.image()).isTrue();

		FileDownload download = postService.getDownload(postId, attachment.id());
		assertThat(download.resource().getContentAsByteArray()).isEqualTo("data".getBytes());
	}

	@Test
	void F10_파일_없이_작성하면_첨부가_없다() {
		Long postId = postService.write(writer, new PostForm("제목", "본문"));

		assertThat(postService.getAttachment(postId)).isEmpty();
	}

	@Test
	void F10_수정시_새_파일을_올리면_기존_파일이_교체된다() {
		Long postId = postService.write(writer, formWithFile("old.pdf"));
		Long oldFileId = postService.getAttachment(postId).orElseThrow().id();

		postService.update(postId, writer, formWithFile("new.txt"));

		PostFileResponse attachment = postService.getAttachment(postId).orElseThrow();
		assertThat(attachment.originalName()).isEqualTo("new.txt");
		assertThat(postFileRepository.findByIdAndPostId(oldFileId, postId)).isEmpty();
	}

	@Test
	void F10_수정시_기존_파일_삭제를_체크하면_첨부가_지워진다() {
		Long postId = postService.write(writer, formWithFile("old.pdf"));
		PostForm form = new PostForm("제목", "본문");
		form.setDeleteFile(true);

		postService.update(postId, writer, form);

		assertThat(postService.getAttachment(postId)).isEmpty();
	}

	@Test
	void F10_수정시_파일을_건드리지_않으면_기존_첨부가_유지된다() {
		Long postId = postService.write(writer, formWithFile("old.pdf"));

		postService.update(postId, writer, new PostForm("새 제목", "새 본문"));

		assertThat(postService.getAttachment(postId).orElseThrow().originalName()).isEqualTo("old.pdf");
	}

	@Test
	void F10_작성자가_아니면_첨부를_바꿀_수_없다() {
		Long postId = postService.write(writer, formWithFile("old.pdf"));

		assertThatThrownBy(() -> postService.update(postId, other, formWithFile("hack.txt")))
				.isInstanceOf(ForbiddenException.class);
		assertThat(postService.getAttachment(postId).orElseThrow().originalName()).isEqualTo("old.pdf");
	}

	@Test
	void F10_글을_삭제하면_첨부도_삭제된다() {
		Long postId = postService.write(writer, formWithFile("old.pdf"));

		postService.delete(postId, writer);

		assertThat(postFileRepository.findByPostId(postId)).isEmpty();
	}

	@Test
	void F10_다른_글의_파일이나_이미지가_아닌_파일의_미리보기는_404() {
		Long postId = postService.write(writer, formWithFile("doc.pdf"));
		Long otherPostId = postService.write(writer, new PostForm("다른 글", "본문"));
		PostFile file = postFileRepository.findByPostId(postId).orElseThrow();

		assertThatThrownBy(() -> postService.getDownload(otherPostId, file.getId()))
				.isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> postService.getPreview(postId, file.getId()))
				.isInstanceOf(NotFoundException.class);
	}
}

package com.himedia.board.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.himedia.board.global.session.LoginMember;
import com.himedia.board.global.session.SessionConst;
import com.himedia.board.member.domain.Member;
import com.himedia.board.member.repository.MemberRepository;
import com.himedia.board.post.dto.PostForm;
import com.himedia.board.post.repository.PostFileRepository;
import com.himedia.board.post.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** F-10 첨부 파일: 작성/수정 폼 업로드, 상세 표시, 다운로드/미리보기 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostFileControllerTest {

	private static final byte[] PNG_BYTES = {(byte) 0x89, 'P', 'N', 'G'};

	@Autowired
	MockMvc mockMvc;
	@Autowired
	MemberRepository memberRepository;
	@Autowired
	PostService postService;
	@Autowired
	PostFileRepository postFileRepository;

	MockHttpSession writerSession;
	MockHttpSession otherSession;
	LoginMember writer;
	Long postId;
	Long fileId;

	@BeforeEach
	void setUp() {
		writerSession = loginSession("file-ctrl-w@test.com", "첨부주인");
		otherSession = loginSession("file-ctrl-o@test.com", "첨부남");
		writer = (LoginMember) writerSession.getAttribute(SessionConst.LOGIN_MEMBER);
		PostForm form = new PostForm("이미지 글", "본문");
		form.setFile(png("고양이 사진.png"));
		postId = postService.write(writer, form);
		fileId = postService.getAttachment(postId).orElseThrow().id();
	}

	private MockHttpSession loginSession(String email, String nickname) {
		Long id = memberRepository.save(
				Member.builder().email(email).password("hashed").nickname(nickname).build());
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionConst.LOGIN_MEMBER, new LoginMember(id, email, nickname));
		return session;
	}

	private MockMultipartFile png(String filename) {
		return new MockMultipartFile("file", filename, "image/png", PNG_BYTES);
	}

	// ---------- 작성 ----------

	@Test
	void F10_첨부_파일과_함께_글을_작성한다() throws Exception {
		mockMvc.perform(multipart("/posts").file(new MockMultipartFile("file", "보고서.pdf", "application/pdf", "pdf".getBytes()))
						.session(writerSession).param("title", "첨부 글").param("content", "본문"))
				.andExpect(redirectedUrlPattern("/posts/*"));
	}

	@Test
	void F10_허용되지_않은_형식이나_10MB_초과_파일은_폼을_다시_보여준다() throws Exception {
		mockMvc.perform(multipart("/posts").file(new MockMultipartFile("file", "virus.exe", "application/octet-stream", new byte[] {1}))
						.session(writerSession).param("title", "제목").param("content", "본문"))
				.andExpect(view().name("post/form"))
				.andExpect(model().attributeHasFieldErrors("postForm", "file"))
				.andExpect(content().string(containsString("첨부할 수 없는 파일 형식입니다")));

		byte[] tooBig = new byte[10 * 1024 * 1024 + 1];
		mockMvc.perform(multipart("/posts").file(new MockMultipartFile("file", "big.zip", "application/zip", tooBig))
						.session(writerSession).param("title", "제목").param("content", "본문"))
				.andExpect(view().name("post/form"))
				.andExpect(content().string(containsString("첨부 파일은 10MB 이하만 올릴 수 있습니다")));
	}

	@Test
	void F10_SVG는_첨부할_수_없다() throws Exception {
		mockMvc.perform(multipart("/posts").file(new MockMultipartFile("file", "x.svg", "image/svg+xml", "<svg/>".getBytes()))
						.session(writerSession).param("title", "제목").param("content", "본문"))
				.andExpect(model().attributeHasFieldErrors("postForm", "file"));
	}

	// ---------- 상세 화면 표시 ----------

	@Test
	void F10_로그인하면_상세에_다운로드_링크와_이미지_미리보기가_보인다() throws Exception {
		mockMvc.perform(get("/posts/{id}", postId).session(otherSession))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("고양이 사진.png")))
				.andExpect(content().string(containsString("/posts/" + postId + "/files/" + fileId + "\"")))
				.andExpect(content().string(containsString("/posts/" + postId + "/files/" + fileId + "/preview")));
	}

	@Test
	void F10_비로그인은_파일명만_보이고_미리보기는_보이지_않는다() throws Exception {
		mockMvc.perform(get("/posts/{id}", postId))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("고양이 사진.png")))
				.andExpect(content().string(containsString("로그인 후")))
				.andExpect(content().string(not(containsString("/preview"))));
	}

	// ---------- 다운로드 / 미리보기 ----------

	@Test
	void F10_비로그인은_다운로드할_수_없고_로그인_후_돌아온다() throws Exception {
		mockMvc.perform(get("/posts/{postId}/files/{fileId}", postId, fileId))
				.andExpect(redirectedUrl("/login?redirectURL=%2Fposts%2F" + postId + "%2Ffiles%2F" + fileId));
		mockMvc.perform(get("/posts/{postId}/files/{fileId}/preview", postId, fileId))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	void F10_로그인하면_원래_파일명으로_다운로드한다() throws Exception {
		mockMvc.perform(get("/posts/{postId}/files/{fileId}", postId, fileId).session(otherSession))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "application/octet-stream"))
				.andExpect(header().string("Content-Disposition", containsString("attachment")))
				.andExpect(header().string("Content-Disposition",
						containsString("filename*=UTF-8''%EA%B3%A0%EC%96%91%EC%9D%B4%20%EC%82%AC%EC%A7%84.png")))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(content().bytes(PNG_BYTES));
	}

	@Test
	void F10_이미지는_inline으로_미리보기한다() throws Exception {
		mockMvc.perform(get("/posts/{postId}/files/{fileId}/preview", postId, fileId).session(otherSession))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "image/png"))
				.andExpect(header().string("Content-Disposition", containsString("inline")))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"));
	}

	@Test
	void F10_이미지가_아니거나_다른_글의_파일이면_404() throws Exception {
		PostForm form = new PostForm("문서 글", "본문");
		form.setFile(new MockMultipartFile("file", "a.pdf", "application/pdf", "pdf".getBytes()));
		Long docPostId = postService.write(writer, form);
		Long docFileId = postService.getAttachment(docPostId).orElseThrow().id();

		mockMvc.perform(get("/posts/{postId}/files/{fileId}/preview", docPostId, docFileId).session(otherSession))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/posts/{postId}/files/{fileId}", docPostId, fileId).session(otherSession))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/posts/{postId}/files/{fileId}", postId, Long.MAX_VALUE).session(otherSession))
				.andExpect(status().isNotFound());
	}

	// ---------- 수정 (비로그인 / 타인 / 본인) ----------

	@Test
	void F10_수정_폼에_기존_파일과_삭제_체크박스가_보인다() throws Exception {
		mockMvc.perform(get("/posts/{id}/edit", postId).session(writerSession))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("기존 파일 삭제")))
				.andExpect(content().string(containsString("고양이 사진.png")))
				.andExpect(content().string(containsString("multipart/form-data")));
	}

	@Test
	void F10_비로그인은_첨부를_수정할_수_없다() throws Exception {
		mockMvc.perform(multipart("/posts/{id}/edit", postId).file(png("new.png"))
						.param("title", "x").param("content", "y"))
				.andExpect(redirectedUrl("/login"));
		assertThat(postService.getAttachment(postId).orElseThrow().id()).isEqualTo(fileId);
	}

	@Test
	void F10_작성자가_아니면_첨부를_수정할_수_없다() throws Exception {
		mockMvc.perform(multipart("/posts/{id}/edit", postId).session(otherSession)
						.param("title", "x").param("content", "y").param("deleteFile", "true"))
				.andExpect(status().isForbidden());
		assertThat(postService.getAttachment(postId).orElseThrow().id()).isEqualTo(fileId);
	}

	@Test
	void F10_작성자는_새_파일로_교체하거나_삭제할_수_있다() throws Exception {
		mockMvc.perform(multipart("/posts/{id}/edit", postId).file(png("new.png")).session(writerSession)
						.param("title", "제목").param("content", "본문"))
				.andExpect(redirectedUrl("/posts/" + postId));
		assertThat(postService.getAttachment(postId).orElseThrow().originalName()).isEqualTo("new.png");

		mockMvc.perform(multipart("/posts/{id}/edit", postId).session(writerSession)
						.param("title", "제목").param("content", "본문").param("deleteFile", "true"))
				.andExpect(redirectedUrl("/posts/" + postId));
		assertThat(postFileRepository.findByPostId(postId)).isEmpty();
	}
}

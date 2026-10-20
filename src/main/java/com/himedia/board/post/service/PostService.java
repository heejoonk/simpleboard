package com.himedia.board.post.service;

import com.himedia.board.global.common.PageResult;
import com.himedia.board.global.exception.ForbiddenException;
import com.himedia.board.global.exception.NotFoundException;
import com.himedia.board.global.session.LoginMember;
import com.himedia.board.post.domain.AttachmentPolicy;
import com.himedia.board.post.domain.Post;
import com.himedia.board.post.domain.PostFile;
import com.himedia.board.post.dto.FileDownload;
import com.himedia.board.post.dto.PostDetailResponse;
import com.himedia.board.post.dto.PostFileResponse;
import com.himedia.board.post.dto.PostForm;
import com.himedia.board.post.dto.PostListResponse;
import com.himedia.board.post.repository.PostFileRepository;
import com.himedia.board.post.repository.PostRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

	public static final int PAGE_SIZE = 10;

	private final PostRepository postRepository;
	private final PostFileRepository postFileRepository;
	private final FileStore fileStore;

	@Transactional
	public Long write(LoginMember loginMember, PostForm form) {
		Post post = Post.builder()
				.memberId(loginMember.id())
				.title(form.getTitle())
				.content(form.getContent())
				.build();
		Long postId = postRepository.save(post);
		if (form.hasFile()) {
			attach(postId, form.getFile());
		}
		return postId;
	}

	/** 음수 페이지는 0으로, 범위를 넘는 페이지는 빈 목록으로 처리한다. 쿼리는 목록 1 + COUNT 1. */
	public PageResult<PostListResponse> getPage(int page) {
		int safePage = Math.max(page, 0);
		List<PostListResponse> content = postRepository.findPage((long) safePage * PAGE_SIZE, PAGE_SIZE);
		long total = postRepository.count();
		return PageResult.of(content, safePage, PAGE_SIZE, total);
	}

	/** 상세 조회. 조회수를 1 올린 뒤 조회한다. */
	@Transactional
	public PostDetailResponse getDetail(Long id, LoginMember loginMember) {
		if (postRepository.increaseViewCount(id) == 0) {
			throw notFound();
		}
		PostDetailResponse detail = postRepository.findDetailById(id).orElseThrow(this::notFound);
		return detail.withOwner(loginMember == null ? null : loginMember.id());
	}

	/** 글의 첨부 파일 정보 (없으면 empty). 상세/수정 화면에서 사용한다. */
	public Optional<PostFileResponse> getAttachment(Long postId) {
		return postFileRepository.findByPostId(postId).map(PostFileResponse::from);
	}

	/** 다운로드할 파일. 파일이 URL의 글에 속하지 않으면 404. */
	public FileDownload getDownload(Long postId, Long fileId) {
		PostFile file = postFileRepository.findByIdAndPostId(fileId, postId)
				.orElseThrow(() -> new NotFoundException("첨부 파일이 없습니다."));
		return new FileDownload(file.getOriginalName(), file.getFileSize(), fileStore.load(file.getStoredName()));
	}

	/** 미리보기할 이미지 파일. 이미지가 아니면 404. */
	public FileDownload getPreview(Long postId, Long fileId) {
		FileDownload download = getDownload(postId, fileId);
		if (!AttachmentPolicy.isImage(download.originalName())) {
			throw new NotFoundException("미리보기를 지원하지 않는 파일입니다.");
		}
		return download;
	}

	/** 수정 폼에 채울 기존 값. 작성자가 아니면 403. */
	public PostForm getEditForm(Long id, LoginMember loginMember) {
		Post post = getOwnedPost(id, loginMember);
		return new PostForm(post.getTitle(), post.getContent());
	}

	/** 새 파일을 올리면 기존 첨부를 교체하고, "기존 파일 삭제"를 체크하면 기존 첨부를 지운다. */
	@Transactional
	public void update(Long id, LoginMember loginMember, PostForm form) {
		getOwnedPost(id, loginMember);
		if (postRepository.update(id, form.getTitle(), form.getContent()) == 0) {
			throw notFound();
		}
		if (form.isDeleteFile() || form.hasFile()) {
			removeAttachment(id);
		}
		if (form.hasFile()) {
			attach(id, form.getFile());
		}
	}

	/** 댓글과 첨부 행은 FK CASCADE 로 지워지고, 디스크 파일은 커밋 후 지운다. */
	@Transactional
	public void delete(Long id, LoginMember loginMember) {
		getOwnedPost(id, loginMember);
		Optional<PostFile> attachment = postFileRepository.findByPostId(id);
		if (postRepository.deleteById(id) == 0) {
			throw notFound();
		}
		attachment.ifPresent(file -> fileStore.deleteAfterCommit(file.getStoredName()));
	}

	private void attach(Long postId, MultipartFile file) {
		String storedName = fileStore.store(file);
		postFileRepository.save(PostFile.builder()
				.postId(postId)
				.originalName(PostForm.cleanFilename(file.getOriginalFilename()))
				.storedName(storedName)
				.fileSize(file.getSize())
				.build());
	}

	private void removeAttachment(Long postId) {
		postFileRepository.findByPostId(postId).ifPresent(file -> {
			postFileRepository.deleteById(file.getId());
			fileStore.deleteAfterCommit(file.getStoredName());
		});
	}

	private Post getOwnedPost(Long id, LoginMember loginMember) {
		Post post = postRepository.findById(id).orElseThrow(this::notFound);
		if (!post.getMemberId().equals(loginMember.id())) {
			throw new ForbiddenException("게시글 작성자가 아닙니다. postId=" + id);
		}
		return post;
	}

	private NotFoundException notFound() {
		return new NotFoundException("게시글이 없습니다.");
	}
}

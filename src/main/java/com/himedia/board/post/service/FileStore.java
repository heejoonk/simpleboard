package com.himedia.board.post.service;

import com.himedia.board.global.config.FileProperties;
import com.himedia.board.global.exception.NotFoundException;
import com.himedia.board.post.domain.AttachmentPolicy;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * 첨부 파일을 디스크에 저장/조회/삭제한다.
 * 저장 파일명은 UUID + 확장자로 만들어 사용자가 보낸 파일명을 경로에 쓰지 않는다.
 * DB와 디스크가 어긋나지 않도록 삭제는 커밋 후에, 저장 취소는 롤백 시에 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileStore {

	private final FileProperties fileProperties;

	/** 파일을 저장하고 저장 파일명을 돌려준다. 트랜잭션이 롤백되면 저장한 파일을 지운다. */
	public String store(MultipartFile file) {
		String storedName = UUID.randomUUID() + "." + AttachmentPolicy.extensionOf(file.getOriginalFilename());
		Path target = resolve(storedName);
		try (InputStream in = file.getInputStream()) {
			Files.createDirectories(target.getParent());
			Files.copy(in, target);
		} catch (IOException e) {
			throw new UncheckedIOException("첨부 파일 저장 실패: " + storedName, e);
		}
		runAfterCompletion(storedName, TransactionSynchronization.STATUS_ROLLED_BACK);
		return storedName;
	}

	public Resource load(String storedName) {
		Path path = resolve(storedName);
		if (!Files.isRegularFile(path)) {
			throw new NotFoundException("첨부 파일이 디스크에 없습니다. storedName=" + storedName);
		}
		return new FileSystemResource(path);
	}

	/** 트랜잭션이 커밋된 뒤 파일을 지운다. (트랜잭션 밖이면 바로 지운다) */
	public void deleteAfterCommit(String storedName) {
		runAfterCompletion(storedName, TransactionSynchronization.STATUS_COMMITTED);
	}

	private void runAfterCompletion(String storedName, int expectedStatus) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			if (expectedStatus == TransactionSynchronization.STATUS_COMMITTED) {
				delete(storedName);
			}
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status == expectedStatus) {
					delete(storedName);
				}
			}
		});
	}

	void delete(String storedName) {
		try {
			Files.deleteIfExists(resolve(storedName));
		} catch (IOException e) {
			log.warn("첨부 파일 삭제 실패: {}", storedName, e);
		}
	}

	/** 업로드 폴더 밖을 가리키는 이름(../ 등)은 거부한다. */
	private Path resolve(String storedName) {
		Path baseDir = Path.of(fileProperties.uploadDir()).toAbsolutePath().normalize();
		Path path = baseDir.resolve(storedName).normalize();
		if (!path.getParent().equals(baseDir)) {
			throw new IllegalArgumentException("잘못된 파일 이름입니다: " + storedName);
		}
		return path;
	}
}

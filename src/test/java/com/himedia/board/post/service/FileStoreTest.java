package com.himedia.board.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.himedia.board.global.config.FileProperties;
import com.himedia.board.global.exception.NotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FileStoreTest {

	@TempDir
	Path uploadDir;

	FileStore fileStore;

	@BeforeEach
	void setUp() {
		fileStore = new FileStore(new FileProperties(uploadDir.toString()));
	}

	@Test
	void F10_파일은_UUID_이름으로_업로드_폴더에_저장된다() throws Exception {
		String storedName = fileStore.store(new MockMultipartFile("file", "../../원본 사진.PNG", "image/png", new byte[] {1, 2, 3}));

		assertThat(storedName).matches("[0-9a-f-]{36}\\.png");
		assertThat(Files.readAllBytes(uploadDir.resolve(storedName))).containsExactly(1, 2, 3);
		assertThat(fileStore.load(storedName).contentLength()).isEqualTo(3);
	}

	@Test
	void F10_트랜잭션_밖에서_삭제하면_바로_지워진다() {
		String storedName = fileStore.store(new MockMultipartFile("file", "a.txt", "text/plain", "hi".getBytes()));

		fileStore.deleteAfterCommit(storedName);

		assertThat(uploadDir.resolve(storedName)).doesNotExist();
		assertThatThrownBy(() -> fileStore.load(storedName)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void F10_업로드_폴더_밖을_가리키는_이름은_거부한다() {
		assertThatThrownBy(() -> fileStore.load("../application.yaml")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> fileStore.load("sub/a.png")).isInstanceOf(IllegalArgumentException.class);
	}
}

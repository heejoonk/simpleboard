package com.himedia.board.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yaml 의 file.* 설정. upload-dir 이 없으면 ./uploads 를 사용한다. */
@ConfigurationProperties(prefix = "file")
public record FileProperties(String uploadDir) {

	public FileProperties {
		if (uploadDir == null || uploadDir.isBlank()) {
			uploadDir = "./uploads";
		}
	}
}

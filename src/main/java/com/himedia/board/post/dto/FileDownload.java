package com.himedia.board.post.dto;

import org.springframework.core.io.Resource;

/** 다운로드/미리보기 응답을 만들 때 필요한 정보 */
public record FileDownload(String originalName, long fileSize, Resource resource) {
}

package com.himedia.board.global.common;

import java.util.List;

/**
 * Spring Data의 Page 대신 사용하는 페이지 결과. page는 0부터 시작한다.
 */
public record PageResult<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean hasPrevious,
		boolean hasNext) {

	private static final int PAGE_BLOCK_SIZE = 5;

	public static <T> PageResult<T> of(List<T> content, int page, int size, long totalElements) {
		int totalPages = (int) ((totalElements + size - 1) / size);
		return new PageResult<>(content, page, size, totalElements, totalPages,
				page > 0 && totalPages > 0, page + 1 < totalPages);
	}

	/** 하단 페이지 번호 블록의 시작 페이지 (0부터, 5개씩 묶음) */
	public int blockStart() {
		int lastPage = Math.max(totalPages - 1, 0);
		return (Math.min(page, lastPage) / PAGE_BLOCK_SIZE) * PAGE_BLOCK_SIZE;
	}

	/** 하단 페이지 번호 블록의 마지막 페이지 (0부터, 포함) */
	public int blockEnd() {
		return Math.min(blockStart() + PAGE_BLOCK_SIZE, totalPages) - 1;
	}
}

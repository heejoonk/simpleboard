package com.himedia.board.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

	@ResponseStatus(HttpStatus.NOT_FOUND)
	@ExceptionHandler(NotFoundException.class)
	public String handleNotFound(NotFoundException e) {
		return "error/404";
	}

	/** /posts/abc 처럼 경로의 id가 숫자가 아니면 없는 페이지로 처리한다. */
	@ResponseStatus(HttpStatus.NOT_FOUND)
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public String handleTypeMismatch(MethodArgumentTypeMismatchException e) {
		return "error/404";
	}

	@ResponseStatus(HttpStatus.FORBIDDEN)
	@ExceptionHandler(ForbiddenException.class)
	public String handleForbidden(ForbiddenException e) {
		return "error/403";
	}

	/** 첨부 파일이 multipart 용량 제한(10MB)을 넘으면 컨트롤러에 도달하기 전에 발생한다. (F-10) */
	@ResponseStatus(HttpStatus.CONTENT_TOO_LARGE)
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public String handleMaxUploadSize(MaxUploadSizeExceededException e) {
		return "error/413";
	}

	/**
	 * DataAccessException 등 나머지 예외는 500. 스택트레이스는 서버 로그에만 남긴다.
	 * 단, 스프링 MVC 표준 예외(없는 경로, 허용되지 않은 메서드 등 4xx)는 원래 상태 코드를 유지한다.
	 */
	@ExceptionHandler(Exception.class)
	public String handleException(Exception e, HttpServletRequest request, HttpServletResponse response) {
		if (e instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
			response.setStatus(errorResponse.getStatusCode().value());
			return "error/404";
		}
		log.error("처리되지 않은 예외: {} {}", request.getMethod(), request.getRequestURI(), e);
		response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		return "error/500";
	}
}

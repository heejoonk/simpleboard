package com.himedia.board.post.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 첨부 파일 형식/용량/파일명 길이 검증 (F-10). 파일을 선택하지 않았으면 통과한다. */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AllowedAttachmentValidator.class)
public @interface AllowedAttachment {

	String message() default "첨부할 수 없는 파일입니다";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}

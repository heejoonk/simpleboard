package com.himedia.board.post.dto;

import com.himedia.board.post.domain.AttachmentPolicy;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

public class AllowedAttachmentValidator implements ConstraintValidator<AllowedAttachment, MultipartFile> {

	@Override
	public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
		if (file == null || file.isEmpty()) {
			return true;
		}
		String name = PostForm.cleanFilename(file.getOriginalFilename());
		if (!AttachmentPolicy.isAllowed(name)) {
			return reject(context, "첨부할 수 없는 파일 형식입니다 (허용: " + AttachmentPolicy.ALLOWED_EXTENSIONS_TEXT + ")");
		}
		if (file.getSize() > AttachmentPolicy.MAX_FILE_SIZE) {
			return reject(context, "첨부 파일은 10MB 이하만 올릴 수 있습니다");
		}
		if (name.length() > AttachmentPolicy.MAX_NAME_LENGTH) {
			return reject(context, "파일명은 255자 이하로 해주세요");
		}
		return true;
	}

	private boolean reject(ConstraintValidatorContext context, String message) {
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
		return false;
	}
}

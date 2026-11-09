package com.jjonggeut.domain.auth.exception;

import com.jjonggeut.domain.auth.controller.AuthController;
import com.jjonggeut.global.response.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 동시 가입으로 사전 중복 검사를 통과한 뒤 DB UNIQUE 제약에 걸린 경우를 409로 변환한다.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackageClasses = AuthController.class)
public class AuthExceptionHandler {

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleDuplicate(DataIntegrityViolationException e) {
		String cause = String.valueOf(e.getMostSpecificCause().getMessage()).toLowerCase();
		AuthErrorCode errorCode = cause.contains("nickname")
				? AuthErrorCode.NICKNAME_DUPLICATED
				: AuthErrorCode.LOGIN_ID_DUPLICATED;
		return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.failure(errorCode));
	}
}

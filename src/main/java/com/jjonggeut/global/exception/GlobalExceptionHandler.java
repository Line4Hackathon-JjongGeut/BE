package com.jjonggeut.global.exception;

import com.jjonggeut.global.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
		BaseErrorCode errorCode = e.getErrorCode();
		return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.failure(errorCode));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return build(ErrorCode.VALIDATION_FAILED, message.isBlank() ? ErrorCode.VALIDATION_FAILED.getMessage() : message);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
		String message = e.getConstraintViolations().stream()
				.map(v -> v.getPropertyPath() + ": " + v.getMessage())
				.collect(Collectors.joining(", "));
		return build(ErrorCode.VALIDATION_FAILED, message.isBlank() ? ErrorCode.VALIDATION_FAILED.getMessage() : message);
	}

	@ExceptionHandler({
			HttpMessageNotReadableException.class,
			MethodArgumentTypeMismatchException.class,
			MissingServletRequestParameterException.class
	})
	public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
		log.warn("Bad request: {}", e.getMessage());
		return build(ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.getMessage());
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
		return build(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.getMessage());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
		return build(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getMessage());
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException e) {
		return build(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getMessage());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
		return build(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
		log.error("Unexpected error", e);
		return build(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
	}

	private ResponseEntity<ApiResponse<Void>> build(BaseErrorCode errorCode, String message) {
		return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.failure(errorCode, message));
	}
}

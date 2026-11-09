package com.jjonggeut.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 도메인별 에러 코드 enum(AuthErrorCode 등)이 구현하는 공통 인터페이스.
 */
public interface BaseErrorCode {

	HttpStatus getHttpStatus();

	String getCode();

	String getMessage();
}

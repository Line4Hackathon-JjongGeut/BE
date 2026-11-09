package com.jjonggeut.domain.auth.exception;

import com.jjonggeut.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements BaseErrorCode {

	LOGIN_ID_DUPLICATED(HttpStatus.CONFLICT, "AUTH_409_1", "이미 사용 중인 아이디입니다."),
	NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "AUTH_409_2", "이미 사용 중인 닉네임입니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	AuthErrorCode(HttpStatus httpStatus, String code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}

	@Override
	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return message;
	}
}

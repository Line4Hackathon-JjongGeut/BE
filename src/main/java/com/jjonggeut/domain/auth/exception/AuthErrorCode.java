package com.jjonggeut.domain.auth.exception;

import com.jjonggeut.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements BaseErrorCode {

	LOGIN_ID_DUPLICATED(HttpStatus.CONFLICT, "AUTH_409_1", "이미 사용 중인 아이디입니다."),
	NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "AUTH_409_2", "이미 사용 중인 닉네임입니다."),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_401_1", "아이디 또는 비밀번호가 올바르지 않습니다."),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_2", "유효하지 않거나 만료된 리프레시 토큰입니다."),
	CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_400_1", "현재 비밀번호가 올바르지 않습니다."),
	SAME_PASSWORD(HttpStatus.BAD_REQUEST, "AUTH_400_2", "새 비밀번호는 현재 비밀번호와 달라야 합니다."),
	INVALID_RESET_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_3", "유효하지 않거나 만료된 재설정 토큰입니다."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_404_1", "일치하는 회원 정보가 없습니다."),
	ACCOUNT_UNAVAILABLE(HttpStatus.FORBIDDEN, "AUTH_403_1", "이용할 수 없는 계정입니다.");

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

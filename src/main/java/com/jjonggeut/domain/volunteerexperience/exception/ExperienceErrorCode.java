package com.jjonggeut.domain.volunteerexperience.exception;

import com.jjonggeut.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

public enum ExperienceErrorCode implements BaseErrorCode {

	EXPERIENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "EXPERIENCE_404_1", "봉사 경험을 찾을 수 없습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	ExperienceErrorCode(HttpStatus httpStatus, String code, String message) {
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

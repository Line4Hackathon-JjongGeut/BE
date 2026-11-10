package com.jjonggeut.domain.shelter.exception;

import com.jjonggeut.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

public enum ShelterErrorCode implements BaseErrorCode {

	SHELTER_NOT_FOUND(HttpStatus.NOT_FOUND, "SHELTER_404_1", "보호시설을 찾을 수 없습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	ShelterErrorCode(HttpStatus httpStatus, String code, String message) {
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

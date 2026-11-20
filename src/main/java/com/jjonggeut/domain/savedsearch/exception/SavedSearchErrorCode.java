package com.jjonggeut.domain.savedsearch.exception;

import com.jjonggeut.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

public enum SavedSearchErrorCode implements BaseErrorCode {

	SAVED_SEARCH_NOT_FOUND(HttpStatus.NOT_FOUND, "SAVED_SEARCH_404_1", "저장된 검색 조건을 찾을 수 없습니다."),
	NAME_DUPLICATED(HttpStatus.CONFLICT, "SAVED_SEARCH_409_1", "이미 같은 이름의 검색 조건이 있습니다."),
	LIMIT_EXCEEDED(HttpStatus.CONFLICT, "SAVED_SEARCH_409_2", "검색 조건은 최대 10개까지 저장할 수 있습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	SavedSearchErrorCode(HttpStatus httpStatus, String code, String message) {
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

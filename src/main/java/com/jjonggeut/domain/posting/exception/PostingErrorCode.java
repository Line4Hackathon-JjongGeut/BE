package com.jjonggeut.domain.posting.exception;

import com.jjonggeut.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

public enum PostingErrorCode implements BaseErrorCode {

	POSTING_NOT_FOUND(HttpStatus.NOT_FOUND, "POSTING_404_1", "모집공고를 찾을 수 없습니다."),
	BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "POSTING_404_2", "관심 공고로 저장되어 있지 않습니다."),
	UNSUPPORTED_POSTING_TYPE(HttpStatus.BAD_REQUEST, "POSTING_400_1", "이 공고에서는 사용할 수 없는 기능입니다."),
	BOOKMARK_ALREADY_EXISTS(HttpStatus.CONFLICT, "POSTING_409_1", "이미 관심 공고로 저장되어 있습니다."),
	REPORT_ALREADY_RECEIVED(HttpStatus.CONFLICT, "POSTING_409_2", "같은 유형으로 접수된 신고가 이미 있습니다."),
	INQUIRY_ALREADY_RECEIVED(HttpStatus.CONFLICT, "POSTING_409_3", "이미 참여 의사를 전달했습니다."),
	POSTING_CLOSED(HttpStatus.CONFLICT, "POSTING_409_4", "모집이 종료된 공고입니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	PostingErrorCode(HttpStatus httpStatus, String code, String message) {
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

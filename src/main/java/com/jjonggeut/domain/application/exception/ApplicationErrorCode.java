package com.jjonggeut.domain.application.exception;

import com.jjonggeut.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

public enum ApplicationErrorCode implements BaseErrorCode {

	APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "APPLICATION_404_1", "봉사 신청을 찾을 수 없습니다."),
	SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "APPLICATION_404_2", "봉사 일정을 찾을 수 없습니다."),
	EXPERIENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "APPLICATION_404_3", "첨부하려는 봉사 경험을 찾을 수 없습니다."),
	INVALID_ANSWER(HttpStatus.BAD_REQUEST, "APPLICATION_400_1", "신청서 답변이 질문과 맞지 않습니다."),
	ALREADY_APPLIED(HttpStatus.CONFLICT, "APPLICATION_409_1", "이미 신청한 일정입니다."),
	SCHEDULE_FULL(HttpStatus.CONFLICT, "APPLICATION_409_2", "정원이 가득 찬 일정입니다."),
	NOT_ACCEPTING(HttpStatus.CONFLICT, "APPLICATION_409_3", "신청할 수 없는 일정입니다."),
	NOT_CANCELLABLE_STATUS(HttpStatus.CONFLICT, "APPLICATION_409_4", "취소할 수 없는 상태의 신청입니다."),
	CANCEL_DEADLINE_PASSED(HttpStatus.CONFLICT, "APPLICATION_409_5", "취소 가능 기한이 지났습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	ApplicationErrorCode(HttpStatus httpStatus, String code, String message) {
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

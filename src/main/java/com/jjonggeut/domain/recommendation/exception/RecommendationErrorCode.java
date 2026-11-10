package com.jjonggeut.domain.recommendation.exception;

import com.jjonggeut.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

public enum RecommendationErrorCode implements BaseErrorCode {

	ITEM_NOT_IN_BATCH(HttpStatus.BAD_REQUEST, "RECOMMENDATION_400_1", "추천 묶음에 속하지 않는 항목이 있습니다."),
	RECOMMENDATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RECOMMENDATION_404_1", "추천 정보를 찾을 수 없습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	RecommendationErrorCode(HttpStatus httpStatus, String code, String message) {
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

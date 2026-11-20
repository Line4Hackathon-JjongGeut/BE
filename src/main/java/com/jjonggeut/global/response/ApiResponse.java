package com.jjonggeut.global.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jjonggeut.global.exception.BaseErrorCode;

/**
 * 공통 응답 형식.
 * NOTE: 노션 API 명세의 응답 형식이 확정되면 이 클래스만 수정하면 된다.
 */
public record ApiResponse<T>(
		@JsonProperty("isSuccess") boolean isSuccess,
		String code,
		String message,
		T data
) {

	private static final String SUCCESS_CODE = "COMMON_200";
	private static final String SUCCESS_MESSAGE = "요청에 성공했습니다.";

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, data);
	}

	public static ApiResponse<Void> success() {
		return new ApiResponse<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, null);
	}

	public static ApiResponse<Void> failure(BaseErrorCode errorCode) {
		return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
	}

	public static ApiResponse<Void> failure(BaseErrorCode errorCode, String message) {
		return new ApiResponse<>(false, errorCode.getCode(), message, null);
	}
}

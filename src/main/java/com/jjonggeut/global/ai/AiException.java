package com.jjonggeut.global.ai;

/** 외부 AI 호출 실패(키 미설정, 시간 초과, HTTP 오류, 응답 이상). 메시지에 사용자 입력을 담지 않는다. */
public class AiException extends RuntimeException {

	public AiException(String message) {
		super(message);
	}

	public AiException(String message, Throwable cause) {
		super(message, cause);
	}
}

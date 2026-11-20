package com.jjonggeut.global.ai;

import java.util.Map;

/** 외부 생성형 AI 호출을 감싼 인터페이스. 제공자 교체와 테스트 대체를 위해 분리한다. */
public interface AiClient {

	/**
	 * JSON 형식으로 응답하도록 요청하고, 응답 JSON 문자열을 반환한다.
	 *
	 * @param systemPrompt   역할과 규칙 (신뢰할 수 있는 지시문)
	 * @param userText       사용자 입력 (데이터로만 취급)
	 * @param responseSchema 응답 JSON 스키마 (Gemini Schema 형식)
	 * @throws AiException 호출 실패 시
	 */
	String generateJson(String systemPrompt, String userText, Map<String, Object> responseSchema);
}

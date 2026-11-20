package com.jjonggeut.global.ai;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Google Gemini API(generateContent) 호출 구현. API 키는 요청 헤더로만 전달한다. */
@Component
public class GeminiAiClient implements AiClient {

	private final RestClient restClient;
	private final String apiKey;
	private final String model;

	public GeminiAiClient(RestClient geminiRestClient,
			@Value("${ai.api-key:}") String apiKey,
			@Value("${ai.model:gemini-3.5-flash-lite}") String model) {
		this.restClient = geminiRestClient;
		this.apiKey = apiKey;
		this.model = model;
	}

	@Override
	public String generateJson(String systemPrompt, String userText, Map<String, Object> responseSchema) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new AiException("AI API 키가 설정되지 않았습니다.");
		}

		Map<String, Object> body = Map.of(
				"systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
				"contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userText)))),
				"generationConfig", Map.of(
						"responseMimeType", "application/json",
						"responseSchema", responseSchema,
						// 추론 토큰도 출력 한도에 포함되므로 JSON 이 잘리지 않도록 넉넉히 둔다.
						// temperature·thinking 설정은 모델 세대마다 허용 값이 달라 지정하지 않고 기본값을 사용한다.
						"maxOutputTokens", 2048));

		JsonNode response;
		try {
			response = restClient.post()
					.uri("/models/{model}:generateContent", model)
					.header("x-goog-api-key", apiKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.retrieve()
					.body(JsonNode.class);
		} catch (RestClientResponseException e) {
			// 원인 파악을 위해 HTTP 상태 코드만 남긴다. (요청·응답 본문에는 사용자 글이나 키가 있을 수 있어 제외)
			throw new AiException("AI 호출에 실패했습니다. (HTTP " + e.getStatusCode().value() + ")", e);
		} catch (RestClientException e) {
			throw new AiException("AI 호출에 실패했습니다. (" + e.getClass().getSimpleName() + ")", e);
		}

		String text = response == null ? null
				: response.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
		if (text == null || text.isBlank()) {
			throw new AiException("AI 응답에 결과가 없습니다.");
		}
		return text;
	}
}

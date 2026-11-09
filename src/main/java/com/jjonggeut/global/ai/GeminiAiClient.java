package com.jjonggeut.global.ai;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Google Gemini API(generateContent) 호출 구현. API 키는 요청 헤더로만 전달한다. */
@Component
public class GeminiAiClient implements AiClient {

	private final RestClient restClient;
	private final String apiKey;
	private final String model;

	public GeminiAiClient(RestClient geminiRestClient,
			@Value("${ai.api-key:}") String apiKey,
			@Value("${ai.model:gemini-2.5-flash}") String model) {
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
						"temperature", 0.2,
						"maxOutputTokens", 1024,
						// 정리·분류 작업에는 추론이 필요 없어 끄면 빠르고 출력이 잘리지 않는다.
						"thinkingConfig", Map.of("thinkingBudget", 0)));

		JsonNode response;
		try {
			response = restClient.post()
					.uri("/models/{model}:generateContent", model)
					.header("x-goog-api-key", apiKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.retrieve()
					.body(JsonNode.class);
		} catch (RestClientException e) {
			throw new AiException("AI 호출에 실패했습니다.", e);
		}

		String text = response == null ? null
				: response.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
		if (text == null || text.isBlank()) {
			throw new AiException("AI 응답에 결과가 없습니다.");
		}
		return text;
	}
}

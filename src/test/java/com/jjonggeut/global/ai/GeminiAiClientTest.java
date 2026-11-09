package com.jjonggeut.global.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiAiClientTest {

	private static final String URL =
			"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent";

	private MockRestServiceServer server;
	private GeminiAiClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder()
				.baseUrl("https://generativelanguage.googleapis.com/v1beta");
		server = MockRestServiceServer.bindTo(builder).build();
		client = new GeminiAiClient(builder.build(), "test-key", "gemini-3.5-flash-lite");
	}

	@Test
	void 요청_형식을_지키고_응답_텍스트를_반환한다() {
		server.expect(requestTo(URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("x-goog-api-key", "test-key"))
				.andExpect(jsonPath("$.systemInstruction.parts[0].text").value("시스템"))
				.andExpect(jsonPath("$.contents[0].parts[0].text").value("사용자글"))
				.andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
				.andRespond(withSuccess("""
						{"candidates":[{"content":{"parts":[{"text":"{\\"summary\\":\\"정리\\"}"}]}}]}
						""", MediaType.APPLICATION_JSON));

		String result = client.generateJson("시스템", "사용자글", Map.of("type", "OBJECT"));

		assertThat(result).isEqualTo("{\"summary\":\"정리\"}");
		server.verify();
	}

	@Test
	void HTTP_오류는_AiException으로_변환한다() {
		server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

		assertThatThrownBy(() -> client.generateJson("시스템", "사용자글", Map.of()))
				.isInstanceOf(AiException.class);
	}

	@Test
	void 결과_텍스트가_없으면_AiException이다() {
		server.expect(requestTo(URL))
				.andRespond(withSuccess("{\"candidates\":[]}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.generateJson("시스템", "사용자글", Map.of()))
				.isInstanceOf(AiException.class);
	}

	@Test
	void API_키가_없으면_호출하지_않고_AiException이다() {
		RestClient.Builder builder = RestClient.builder()
				.baseUrl("https://generativelanguage.googleapis.com/v1beta");
		MockRestServiceServer noCallServer = MockRestServiceServer.bindTo(builder).build();
		GeminiAiClient noKeyClient = new GeminiAiClient(builder.build(), "", "gemini-3.5-flash-lite");

		assertThatThrownBy(() -> noKeyClient.generateJson("시스템", "사용자글", Map.of()))
				.isInstanceOf(AiException.class);
		noCallServer.verify();
	}
}

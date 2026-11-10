package com.jjonggeut.domain.consent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjonggeut.domain.consent.entity.ConsentType;
import com.jjonggeut.domain.consent.entity.UserConsent;
import com.jjonggeut.domain.consent.repository.UserConsentRepository;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ConsentRecordTest {

	private static final String URL = "/api/v1/users/me/consents";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserConsentRepository consentRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	private User user;
	private User other;
	private String token;
	private String otherToken;

	@BeforeEach
	void setUp() throws Exception {
		user = userRepository.save(User.createUser("jjong123", passwordEncoder.encode("password1"), "김쫑긋", "쫑긋이"));
		other = userRepository.save(User.createUser("other456", passwordEncoder.encode("password1"), "이다른", "다른이"));
		token = login("jjong123");
		otherToken = login("other456");
	}

	private String login(String loginId) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"loginId":"%s","password":"password1"}
								""".formatted(loginId)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).get("data").get("accessToken").asText();
	}

	private ResultActions record(String bearer, String body) throws Exception {
		return mockMvc.perform(post(URL)
				.header("Authorization", "Bearer " + bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	@Test
	void 동의를_기록하면_이력이_저장되고_응답한다() throws Exception {
		record(token, """
				{"consents": [{"consentType": "TERMS", "policyVersion": "2026-11-01", "agreed": true}]}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.consents.length()").value(1))
				.andExpect(jsonPath("$.data.consents[0].consentId").isNumber())
				.andExpect(jsonPath("$.data.consents[0].consentType").value("TERMS"))
				.andExpect(jsonPath("$.data.consents[0].policyVersion").value("2026-11-01"))
				.andExpect(jsonPath("$.data.consents[0].agreed").value(true))
				.andExpect(jsonPath("$.data.consents[0].recordedAt").isNotEmpty());

		assertThat(consentRepository.findAll()).singleElement().satisfies(consent -> {
			assertThat(consent.getUserId()).isEqualTo(user.getUserId());
			assertThat(consent.getConsentType()).isEqualTo(ConsentType.TERMS);
			assertThat(consent.isAgreed()).isTrue();
		});
	}

	@Test
	void 여러_종류를_한_번에_기록하면_요청_순서대로_응답한다() throws Exception {
		record(token, """
				{"consents": [
				  {"consentType": "TERMS", "policyVersion": "2026-11-01", "agreed": true},
				  {"consentType": "PRIVACY", "policyVersion": "2026-11-01", "agreed": true},
				  {"consentType": "LOCATION", "policyVersion": "2026-11-01", "agreed": false}
				]}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.consents.length()").value(3))
				.andExpect(jsonPath("$.data.consents[0].consentType").value("TERMS"))
				.andExpect(jsonPath("$.data.consents[1].consentType").value("PRIVACY"))
				.andExpect(jsonPath("$.data.consents[2].consentType").value("LOCATION"))
				.andExpect(jsonPath("$.data.consents[2].agreed").value(false));

		assertThat(consentRepository.countByUserId(user.getUserId())).isEqualTo(3);
	}

	@Test
	void 철회는_기존_이력을_바꾸지_않고_새_행으로_추가된다() throws Exception {
		record(token, """
				{"consents": [{"consentType": "LOCATION", "policyVersion": "2026-11-01", "agreed": true}]}
				""").andExpect(status().isOk());
		record(token, """
				{"consents": [{"consentType": "LOCATION", "policyVersion": "2026-11-01", "agreed": false}]}
				""").andExpect(status().isOk());

		assertThat(consentRepository.findAll())
				.extracting(UserConsent::isAgreed)
				.containsExactlyInAnyOrder(true, false);
	}

	@Test
	void 같은_내용을_다시_기록해도_이력이_계속_쌓인다() throws Exception {
		String body = """
				{"consents": [{"consentType": "PRIVACY", "policyVersion": "2026-11-01", "agreed": true}]}
				""";
		record(token, body).andExpect(status().isOk());
		record(token, body).andExpect(status().isOk());

		assertThat(consentRepository.countByUserId(user.getUserId())).isEqualTo(2);
	}

	@Test
	void 정책_버전이_바뀌면_버전별로_기록된다() throws Exception {
		record(token, """
				{"consents": [{"consentType": "TERMS", "policyVersion": "2026-11-01", "agreed": true}]}
				""").andExpect(status().isOk());
		record(token, """
				{"consents": [{"consentType": "TERMS", "policyVersion": "2027-01-01", "agreed": true}]}
				""").andExpect(status().isOk());

		assertThat(consentRepository.findAll())
				.extracting(UserConsent::getPolicyVersion)
				.containsExactlyInAnyOrder("2026-11-01", "2027-01-01");
	}

	@Test
	void 다른_사용자의_이력과_섞이지_않는다() throws Exception {
		record(token, """
				{"consents": [{"consentType": "TERMS", "policyVersion": "2026-11-01", "agreed": true}]}
				""").andExpect(status().isOk());
		record(otherToken, """
				{"consents": [{"consentType": "TERMS", "policyVersion": "2026-11-01", "agreed": false}]}
				""").andExpect(status().isOk());

		assertThat(consentRepository.countByUserId(user.getUserId())).isEqualTo(1);
		assertThat(consentRepository.countByUserId(other.getUserId())).isEqualTo(1);
	}

	@Test
	void 정책_버전_앞뒤_공백은_제거한다() throws Exception {
		record(token, """
				{"consents": [{"consentType": "TERMS", "policyVersion": "  2026-11-01  ", "agreed": true}]}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.consents[0].policyVersion").value("2026-11-01"));
	}

	@Test
	void 동의_목록이_비어_있으면_400() throws Exception {
		record(token, """
				{"consents": []}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		record(token, "{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 같은_종류를_한_요청에_두_번_보내면_400() throws Exception {
		record(token, """
				{"consents": [
				  {"consentType": "TERMS", "policyVersion": "2026-11-01", "agreed": true},
				  {"consentType": "TERMS", "policyVersion": "2026-11-01", "agreed": false}
				]}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		assertThat(consentRepository.count()).isZero();
	}

	@Test
	void 정책_버전이_비었거나_너무_길면_400() throws Exception {
		record(token, """
				{"consents": [{"consentType": "TERMS", "policyVersion": "  ", "agreed": true}]}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		record(token, """
				{"consents": [{"consentType": "TERMS", "policyVersion": "123456789012345678901", "agreed": true}]}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 동의_여부나_종류가_없으면_400() throws Exception {
		record(token, """
				{"consents": [{"consentType": "TERMS", "policyVersion": "2026-11-01"}]}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		record(token, """
				{"consents": [{"policyVersion": "2026-11-01", "agreed": true}]}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 정의되지_않은_종류면_400() throws Exception {
		record(token, """
				{"consents": [{"consentType": "MARKETING", "policyVersion": "2026-11-01", "agreed": true}]}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 일부라도_검증에_실패하면_아무것도_저장되지_않는다() throws Exception {
		record(token, """
				{"consents": [
				  {"consentType": "TERMS", "policyVersion": "2026-11-01", "agreed": true},
				  {"consentType": "PRIVACY", "policyVersion": "", "agreed": true}
				]}
				""").andExpect(status().isBadRequest());

		assertThat(consentRepository.count()).isZero();
	}

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
						{"consents": [{"consentType": "TERMS", "policyVersion": "2026-11-01", "agreed": true}]}
						"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}
}

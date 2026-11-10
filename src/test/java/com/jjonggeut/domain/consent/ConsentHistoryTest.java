package com.jjonggeut.domain.consent;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class ConsentHistoryTest {

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

	@BeforeEach
	void setUp() throws Exception {
		user = userRepository.save(User.createUser("jjong123", passwordEncoder.encode("password1"), "김쫑긋", "쫑긋이"));
		other = userRepository.save(User.createUser("other456", passwordEncoder.encode("password1"), "이다른", "다른이"));
		token = login("jjong123");
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

	private void save(User owner, ConsentType type, String version, boolean agreed) {
		consentRepository.saveAndFlush(UserConsent.create(owner.getUserId(), type, version, agreed));
	}

	private ResultActions list(String query) throws Exception {
		return mockMvc.perform(get(URL + query).header("Authorization", "Bearer " + token));
	}

	@Test
	void 이력이_없으면_빈_배열로_200() throws Exception {
		list("")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.current").isEmpty())
				.andExpect(jsonPath("$.data.history").isEmpty());
	}

	@Test
	void 이력은_최신순이고_current는_종류별_최근_기록이다() throws Exception {
		save(user, ConsentType.LOCATION, "2026-11-01", true);
		save(user, ConsentType.TERMS, "2026-11-01", true);
		save(user, ConsentType.LOCATION, "2026-11-01", false);

		list("")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.history.length()").value(3))
				.andExpect(jsonPath("$.data.history[0].consentType").value("LOCATION"))
				.andExpect(jsonPath("$.data.history[0].agreed").value(false))
				.andExpect(jsonPath("$.data.history[1].consentType").value("TERMS"))
				.andExpect(jsonPath("$.data.history[2].agreed").value(true))
				.andExpect(jsonPath("$.data.history[0].consentId").isNumber())
				.andExpect(jsonPath("$.data.history[0].recordedAt").isNotEmpty())
				// current 는 동의 종류 정의 순서: TERMS, LOCATION
				.andExpect(jsonPath("$.data.current.length()").value(2))
				.andExpect(jsonPath("$.data.current[0].consentType").value("TERMS"))
				.andExpect(jsonPath("$.data.current[0].agreed").value(true))
				.andExpect(jsonPath("$.data.current[1].consentType").value("LOCATION"))
				.andExpect(jsonPath("$.data.current[1].agreed").value(false));
	}

	@Test
	void 철회_후_다시_동의하면_current가_동의로_바뀐다() throws Exception {
		save(user, ConsentType.AI_PROCESSING, "2026-11-01", true);
		save(user, ConsentType.AI_PROCESSING, "2026-11-01", false);
		save(user, ConsentType.AI_PROCESSING, "2026-11-01", true);

		list("")
				.andExpect(jsonPath("$.data.current.length()").value(1))
				.andExpect(jsonPath("$.data.current[0].consentType").value("AI_PROCESSING"))
				.andExpect(jsonPath("$.data.current[0].agreed").value(true))
				.andExpect(jsonPath("$.data.history.length()").value(3));
	}

	@Test
	void 정책_버전이_바뀌면_current는_최신_버전이다() throws Exception {
		save(user, ConsentType.PRIVACY, "2026-11-01", true);
		save(user, ConsentType.PRIVACY, "2027-01-01", true);

		list("")
				.andExpect(jsonPath("$.data.current.length()").value(1))
				.andExpect(jsonPath("$.data.current[0].policyVersion").value("2027-01-01"))
				.andExpect(jsonPath("$.data.history.length()").value(2))
				.andExpect(jsonPath("$.data.history[1].policyVersion").value("2026-11-01"));
	}

	@Test
	void 종류로_필터링하면_해당_종류만_조회된다() throws Exception {
		save(user, ConsentType.TERMS, "2026-11-01", true);
		save(user, ConsentType.LOCATION, "2026-11-01", false);
		save(user, ConsentType.LOCATION, "2026-11-01", true);

		list("?consentType=LOCATION")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.history.length()").value(2))
				.andExpect(jsonPath("$.data.history[0].consentType").value("LOCATION"))
				.andExpect(jsonPath("$.data.history[1].consentType").value("LOCATION"))
				.andExpect(jsonPath("$.data.current.length()").value(1))
				.andExpect(jsonPath("$.data.current[0].agreed").value(true));
	}

	@Test
	void 필터한_종류의_이력이_없으면_빈_배열이다() throws Exception {
		save(user, ConsentType.TERMS, "2026-11-01", true);

		list("?consentType=THIRD_PARTY")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.current").isEmpty())
				.andExpect(jsonPath("$.data.history").isEmpty());
	}

	@Test
	void 다른_사용자의_이력은_보이지_않는다() throws Exception {
		save(other, ConsentType.TERMS, "2026-11-01", true);
		save(other, ConsentType.PRIVACY, "2026-11-01", false);
		save(user, ConsentType.TERMS, "2026-11-01", false);

		list("")
				.andExpect(jsonPath("$.data.history.length()").value(1))
				.andExpect(jsonPath("$.data.current.length()").value(1))
				.andExpect(jsonPath("$.data.current[0].agreed").value(false));
	}

	@Test
	void 기록_API로_저장한_동의가_조회된다() throws Exception {
		mockMvc.perform(post(URL)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"consents": [
								  {"consentType": "TERMS", "policyVersion": "2026-11-01", "agreed": true},
								  {"consentType": "LOCATION", "policyVersion": "2026-11-01", "agreed": false}
								]}
								"""))
				.andExpect(status().isOk());

		list("")
				.andExpect(jsonPath("$.data.history.length()").value(2))
				.andExpect(jsonPath("$.data.current.length()").value(2))
				.andExpect(jsonPath("$.data.current[0].consentType").value("TERMS"))
				.andExpect(jsonPath("$.data.current[1].consentType").value("LOCATION"))
				.andExpect(jsonPath("$.data.current[1].agreed").value(false));
	}

	@Test
	void 정의되지_않은_종류로_조회하면_400() throws Exception {
		list("?consentType=MARKETING")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(get(URL))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}
}

package com.jjonggeut.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjonggeut.domain.auth.repository.RefreshTokenRepository;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthLogoutTest {

	private static final String LOGIN_URL = "/api/v1/auth/login";
	private static final String LOGOUT_URL = "/api/v1/auth/logout";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	private User user;
	private String accessToken;
	private String refreshToken;

	@BeforeEach
	void setUp() throws Exception {
		user = userRepository.save(
				User.createUser("jjong123", passwordEncoder.encode("password1"), "김쫑긋", "쫑긋이"));

		String response = mockMvc.perform(post(LOGIN_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"loginId":"jjong123","password":"password1"}
								"""))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		JsonNode data = objectMapper.readTree(response).get("data");
		accessToken = data.get("accessToken").asText();
		refreshToken = data.get("refreshToken").asText();
	}

	/** 서명 앞부분의 글자를 바꿔 실제로 서명이 달라지게 한다. */
	private String tamper(String token) {
		int signatureStart = token.lastIndexOf('.') + 1;
		char original = token.charAt(signatureStart);
		char changed = original == 'A' ? 'B' : 'A';
		return token.substring(0, signatureStart) + changed + token.substring(signatureStart + 1);
	}

	@Test
	void 로그아웃하면_리프레시_토큰이_삭제된다() throws Exception {
		assertThat(refreshTokenRepository.findByUserId(user.getUserId())).isPresent();

		mockMvc.perform(post(LOGOUT_URL).header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true));

		assertThat(refreshTokenRepository.findByUserId(user.getUserId())).isEmpty();
	}

	@Test
	void 이미_로그아웃된_상태에서_다시_호출해도_200이다() throws Exception {
		mockMvc.perform(post(LOGOUT_URL).header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk());
		mockMvc.perform(post(LOGOUT_URL).header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk());
	}

	@Test
	void 토큰이_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(post(LOGOUT_URL))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}

	@Test
	void 변조된_토큰은_401을_반환한다() throws Exception {
		mockMvc.perform(post(LOGOUT_URL).header("Authorization", "Bearer " + tamper(accessToken)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}

	@Test
	void 리프레시_토큰으로는_호출할_수_없다() throws Exception {
		mockMvc.perform(post(LOGOUT_URL).header("Authorization", "Bearer " + refreshToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}

	@Test
	void 인증이_필요한_경로는_토큰_없이_접근하면_401이다() throws Exception {
		mockMvc.perform(get("/api/v1/anything"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 허용_경로는_토큰_없이_접근할_수_있다() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}
}

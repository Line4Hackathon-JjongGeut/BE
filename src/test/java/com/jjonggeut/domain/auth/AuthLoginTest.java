package com.jjonggeut.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjonggeut.domain.auth.entity.RefreshToken;
import com.jjonggeut.domain.auth.repository.RefreshTokenRepository;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.entity.UserStatus;
import com.jjonggeut.domain.user.repository.UserRepository;
import com.jjonggeut.global.util.HashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthLoginTest {

	private static final String LOGIN_URL = "/api/v1/auth/login";

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

	@BeforeEach
	void setUp() {
		user = userRepository.save(
				User.createUser("jjong123", passwordEncoder.encode("password1"), "김쫑긋", "쫑긋이"));
	}

	private String body(String loginId, String password) {
		return """
				{"loginId":"%s","password":"%s"}
				""".formatted(loginId, password);
	}

	private String login(String loginId, String password) throws Exception {
		return mockMvc.perform(post(LOGIN_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(loginId, password)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}

	@Test
	void 로그인에_성공하면_토큰을_발급하고_리프레시_토큰은_해시로_저장한다() throws Exception {
		String response = login("jjong123", "password1");
		JsonNode data = objectMapper.readTree(response).get("data");

		assertThat(data.get("accessToken").asText()).isNotBlank();
		assertThat(data.get("tokenType").asText()).isEqualTo("Bearer");
		assertThat(data.get("accessTokenExpiresIn").asLong()).isEqualTo(3600L);
		assertThat(data.get("nickname").asText()).isEqualTo("쫑긋이");

		String refreshToken = data.get("refreshToken").asText();
		RefreshToken saved = refreshTokenRepository.findByUserId(user.getUserId()).orElseThrow();
		assertThat(saved.getTokenHash()).isNotEqualTo(refreshToken);
		assertThat(saved.getTokenHash()).isEqualTo(HashUtil.sha256Hex(refreshToken));
	}

	@Test
	void 재로그인하면_리프레시_토큰은_사용자당_1건으로_교체된다() throws Exception {
		login("jjong123", "password1");
		String first = refreshTokenRepository.findByUserId(user.getUserId()).orElseThrow().getTokenHash();

		login("jjong123", "password1");

		assertThat(refreshTokenRepository.count()).isEqualTo(1);
		String second = refreshTokenRepository.findByUserId(user.getUserId()).orElseThrow().getTokenHash();
		assertThat(second).isNotEqualTo(first);
	}

	@Test
	void 비밀번호가_틀리면_401을_반환한다() throws Exception {
		mockMvc.perform(post(LOGIN_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("jjong123", "wrongpass1")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_401_1"));
	}

	@Test
	void 없는_아이디도_같은_401을_반환한다() throws Exception {
		mockMvc.perform(post(LOGIN_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("nobody99", "password1")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_401_1"))
				.andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
	}

	@Test
	void 정지된_계정은_403을_반환한다() throws Exception {
		ReflectionTestUtils.setField(user, "status", UserStatus.SUSPENDED);
		userRepository.saveAndFlush(user);

		mockMvc.perform(post(LOGIN_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("jjong123", "password1")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("AUTH_403_1"));
	}

	@Test
	void 필수값이_비어있으면_400을_반환한다() throws Exception {
		mockMvc.perform(post(LOGIN_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("", "")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}
}

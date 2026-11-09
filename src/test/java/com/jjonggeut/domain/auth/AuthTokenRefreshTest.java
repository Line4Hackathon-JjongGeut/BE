package com.jjonggeut.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.entity.UserStatus;
import com.jjonggeut.domain.user.repository.UserRepository;
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
class AuthTokenRefreshTest {

	private static final String LOGIN_URL = "/api/v1/auth/login";
	private static final String LOGOUT_URL = "/api/v1/auth/logout";
	private static final String REFRESH_URL = "/api/v1/auth/tokens/refresh";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

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
		JsonNode data = login();
		accessToken = data.get("accessToken").asText();
		refreshToken = data.get("refreshToken").asText();
	}

	private JsonNode login() throws Exception {
		String response = mockMvc.perform(post(LOGIN_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"loginId":"jjong123","password":"password1"}
								"""))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).get("data");
	}

	private org.springframework.test.web.servlet.ResultActions refresh(String token) throws Exception {
		return mockMvc.perform(post(REFRESH_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"refreshToken":"%s"}
						""".formatted(token)));
	}

	@Test
	void 유효한_리프레시_토큰으로_액세스_토큰을_재발급한다() throws Exception {
		String response = refresh(refreshToken)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.data.accessTokenExpiresIn").value(3600))
				.andReturn().getResponse().getContentAsString();

		String newAccessToken = objectMapper.readTree(response).get("data").get("accessToken").asText();
		assertThat(newAccessToken).isNotBlank();

		// 재발급된 액세스 토큰으로 인증이 필요한 API를 호출할 수 있다.
		mockMvc.perform(post(LOGOUT_URL).header("Authorization", "Bearer " + newAccessToken))
				.andExpect(status().isOk());
	}

	@Test
	void 필수값이_비어있으면_400을_반환한다() throws Exception {
		refresh("")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 위조된_토큰은_401을_반환한다() throws Exception {
		refresh("not.a.token")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_401_2"));
	}

	@Test
	void 액세스_토큰으로는_재발급할_수_없다() throws Exception {
		refresh(accessToken)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_401_2"));
	}

	@Test
	void 로그아웃한_뒤에는_재발급할_수_없다() throws Exception {
		mockMvc.perform(post(LOGOUT_URL).header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk());

		refresh(refreshToken)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_401_2"));
	}

	@Test
	void 재로그인하면_이전_리프레시_토큰은_사용할_수_없다() throws Exception {
		String newRefreshToken = login().get("refreshToken").asText();

		refresh(refreshToken)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_401_2"));
		refresh(newRefreshToken)
				.andExpect(status().isOk());
	}

	@Test
	void 정지된_계정은_403을_반환한다() throws Exception {
		ReflectionTestUtils.setField(user, "status", UserStatus.SUSPENDED);
		userRepository.saveAndFlush(user);

		refresh(refreshToken)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("AUTH_403_1"));
	}
}

package com.jjonggeut.domain.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthPasswordChangeTest {

	private static final String LOGIN_URL = "/api/v1/auth/login";
	private static final String REFRESH_URL = "/api/v1/auth/tokens/refresh";
	private static final String RESET_URL = "/api/v1/auth/password/reset-requests";
	private static final String PASSWORD_URL = "/api/v1/auth/password";

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
		JsonNode data = objectMapper.readTree(login("password1")
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("data");
		accessToken = data.get("accessToken").asText();
		refreshToken = data.get("refreshToken").asText();
	}

	private ResultActions login(String password) throws Exception {
		return mockMvc.perform(post(LOGIN_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"loginId":"jjong123","password":"%s"}
						""".formatted(password)));
	}

	private ResultActions changeLoggedIn(String currentPassword, String newPassword) throws Exception {
		return mockMvc.perform(patch(PASSWORD_URL)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"currentPassword":"%s","newPassword":"%s"}
						""".formatted(currentPassword, newPassword)));
	}

	private ResultActions changeWithResetToken(String resetToken, String newPassword) throws Exception {
		return mockMvc.perform(patch(PASSWORD_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"resetToken":"%s","newPassword":"%s"}
						""".formatted(resetToken, newPassword)));
	}

	private String issueResetToken() throws Exception {
		String response = mockMvc.perform(post(RESET_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"loginId":"jjong123","name":"김쫑긋"}
								"""))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).get("data").get("resetToken").asText();
	}

	@Test
	void 로그인_상태에서_비밀번호를_변경하면_새_비밀번호로만_로그인된다() throws Exception {
		changeLoggedIn("password1", "newpass99")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true));

		login("newpass99").andExpect(status().isOk());
		login("password1").andExpect(status().isUnauthorized());
	}

	@Test
	void 비밀번호를_변경하면_기존_리프레시_토큰이_삭제된다() throws Exception {
		assertThat(refreshTokenRepository.findByUserId(user.getUserId())).isPresent();

		changeLoggedIn("password1", "newpass99").andExpect(status().isOk());

		assertThat(refreshTokenRepository.findByUserId(user.getUserId())).isEmpty();
		mockMvc.perform(post(REFRESH_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"%s"}
								""".formatted(refreshToken)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_401_2"));
	}

	@Test
	void 현재_비밀번호가_틀리면_400을_반환한다() throws Exception {
		changeLoggedIn("wrongpass1", "newpass99")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("AUTH_400_1"));
	}

	@Test
	void 로그인_상태에서_현재_비밀번호가_없으면_400을_반환한다() throws Exception {
		mockMvc.perform(patch(PASSWORD_URL)
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"newPassword":"newpass99"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("AUTH_400_1"));
	}

	@Test
	void 새_비밀번호가_현재와_같으면_400을_반환한다() throws Exception {
		changeLoggedIn("password1", "password1")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("AUTH_400_2"));
	}

	@Test
	void 새_비밀번호_형식이_틀리면_400을_반환한다() throws Exception {
		changeLoggedIn("password1", "short")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 재설정_토큰으로_로그인_없이_비밀번호를_변경할_수_있다() throws Exception {
		String resetToken = issueResetToken();

		changeWithResetToken(resetToken, "newpass99").andExpect(status().isOk());

		login("newpass99").andExpect(status().isOk());
	}

	@Test
	void 재설정_토큰은_한_번만_사용할_수_있다() throws Exception {
		String resetToken = issueResetToken();
		changeWithResetToken(resetToken, "newpass99").andExpect(status().isOk());

		changeWithResetToken(resetToken, "another123")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_401_3"));
	}

	@Test
	void 위조된_재설정_토큰은_401을_반환한다() throws Exception {
		changeWithResetToken("not.a.token", "newpass99")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_401_3"));
	}

	@Test
	void 액세스_토큰을_재설정_토큰으로_보낼_수_없다() throws Exception {
		changeWithResetToken(accessToken, "newpass99")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_401_3"));
	}

	@Test
	void 인증도_재설정_토큰도_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(patch(PASSWORD_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"newPassword":"newpass99"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}
}

package com.jjonggeut.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.entity.UserStatus;
import com.jjonggeut.domain.user.repository.UserRepository;
import com.jjonggeut.global.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthPasswordResetRequestTest {

	private static final String RESET_URL = "/api/v1/auth/password/reset-requests";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JwtProvider jwtProvider;

	private User user;

	@BeforeEach
	void setUp() {
		user = userRepository.save(
				User.createUser("jjong123", passwordEncoder.encode("password1"), "김쫑긋", "쫑긋이"));
	}

	private ResultActions request(String loginId, String name) throws Exception {
		return mockMvc.perform(post(RESET_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"loginId":"%s","name":"%s"}
						""".formatted(loginId, name)));
	}

	@Test
	void 아이디와_이름이_일치하면_재설정_토큰을_발급한다() throws Exception {
		String response = request("jjong123", "김쫑긋")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.expiresIn").value(600))
				.andReturn().getResponse().getContentAsString();

		String resetToken = objectMapper.readTree(response).get("data").get("resetToken").asText();
		Claims claims = jwtProvider.parsePasswordResetToken(resetToken);
		assertThat(claims.getSubject()).isEqualTo(String.valueOf(user.getUserId()));
		assertThat(claims.get("pwd", String.class)).hasSize(16);
	}

	@Test
	void 재설정_토큰은_일반_API_인증에_사용할_수_없다() throws Exception {
		String response = request("jjong123", "김쫑긋").andReturn().getResponse().getContentAsString();
		String resetToken = objectMapper.readTree(response).get("data").get("resetToken").asText();

		assertThatThrownBy(() -> jwtProvider.parseAccessToken(resetToken)).isInstanceOf(JwtException.class);
		mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + resetToken))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 필수값이_비어있으면_400을_반환한다() throws Exception {
		request("", "")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 이름이_다르면_404를_반환한다() throws Exception {
		request("jjong123", "다른사람")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("AUTH_404_1"));
	}

	@Test
	void 없는_아이디는_404를_반환한다() throws Exception {
		request("nobody99", "김쫑긋")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("AUTH_404_1"));
	}

	@Test
	void 정지된_계정은_403을_반환한다() throws Exception {
		ReflectionTestUtils.setField(user, "status", UserStatus.SUSPENDED);
		userRepository.saveAndFlush(user);

		request("jjong123", "김쫑긋")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("AUTH_403_1"));
	}
}

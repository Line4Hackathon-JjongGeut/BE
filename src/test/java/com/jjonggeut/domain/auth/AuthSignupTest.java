package com.jjonggeut.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jjonggeut.domain.user.entity.Role;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.entity.UserStatus;
import com.jjonggeut.domain.user.repository.UserRepository;
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
class AuthSignupTest {

	private static final String SIGNUP_URL = "/api/v1/auth/signup";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private String body(String name, String nickname, String loginId, String password) {
		return """
				{"name":"%s","nickname":"%s","loginId":"%s","password":"%s"}
				""".formatted(name, nickname, loginId, password);
	}

	@Test
	void 회원가입에_성공하면_비밀번호는_해시로_저장된다() throws Exception {
		mockMvc.perform(post(SIGNUP_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("김쫑긋", "쫑긋이", "jjong123", "password1")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.loginId").value("jjong123"))
				.andExpect(jsonPath("$.data.nickname").value("쫑긋이"));

		User saved = userRepository.findByLoginId("jjong123").orElseThrow();
		assertThat(saved.getRole()).isEqualTo(Role.USER);
		assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(saved.getPasswordHash()).isNotEqualTo("password1");
		assertThat(passwordEncoder.matches("password1", saved.getPasswordHash())).isTrue();
	}

	@Test
	void 아이디가_중복이면_409를_반환한다() throws Exception {
		userRepository.save(User.createUser("jjong123", passwordEncoder.encode("password1"), "김쫑긋", "쫑긋이"));

		mockMvc.perform(post(SIGNUP_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("이쫑긋", "다른닉", "jjong123", "password1")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("AUTH_409_1"));
	}

	@Test
	void 닉네임이_중복이면_409를_반환한다() throws Exception {
		userRepository.save(User.createUser("jjong123", passwordEncoder.encode("password1"), "김쫑긋", "쫑긋이"));

		mockMvc.perform(post(SIGNUP_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("이쫑긋", "쫑긋이", "other456", "password1")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("AUTH_409_2"));
	}

	@Test
	void 입력값이_규칙에_맞지_않으면_400을_반환한다() throws Exception {
		mockMvc.perform(post(SIGNUP_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("김쫑긋", "쫑긋이", "jjong123", "short")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}
}

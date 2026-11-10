package com.jjonggeut.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjonggeut.domain.auth.repository.RefreshTokenRepository;
import com.jjonggeut.domain.consent.repository.UserConsentRepository;
import com.jjonggeut.domain.profile.repository.UserProfileRepository;
import com.jjonggeut.domain.recommendation.entity.ActivityEventType;
import com.jjonggeut.domain.recommendation.entity.RecommendationBatch;
import com.jjonggeut.domain.recommendation.entity.RecommendationItem;
import com.jjonggeut.domain.recommendation.entity.RecommendationSource;
import com.jjonggeut.domain.recommendation.entity.UserActivityEvent;
import com.jjonggeut.domain.recommendation.repository.RecommendationBatchRepository;
import com.jjonggeut.domain.recommendation.repository.RecommendationItemRepository;
import com.jjonggeut.domain.recommendation.repository.UserActivityEventRepository;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.entity.UserStatus;
import com.jjonggeut.domain.user.repository.UserRepository;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import com.jjonggeut.domain.volunteerexperience.entity.VolunteerExperience;
import com.jjonggeut.domain.volunteerexperience.repository.VolunteerExperienceRepository;
import java.time.LocalDateTime;
import java.util.List;
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
class UserAccountTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private UserProfileRepository profileRepository;

	@Autowired
	private VolunteerExperienceRepository experienceRepository;

	@Autowired
	private RecommendationBatchRepository batchRepository;

	@Autowired
	private RecommendationItemRepository itemRepository;

	@Autowired
	private UserConsentRepository consentRepository;

	@Autowired
	private UserActivityEventRepository eventRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	private User user;
	private String token;

	@BeforeEach
	void setUp() throws Exception {
		user = userRepository.save(User.createUser("jjong123", passwordEncoder.encode("password1"), "김쫑긋", "쫑긋이"));
		userRepository.save(User.createUser("other456", passwordEncoder.encode("password1"), "이다른", "다른이"));
		token = login("jjong123", "password1");
	}

	private String login(String loginId, String password) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"loginId":"%s","password":"%s"}
								""".formatted(loginId, password)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).get("data").get("accessToken").asText();
	}

	private ResultActions me() throws Exception {
		return mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token));
	}

	private ResultActions update(String body) throws Exception {
		return mockMvc.perform(patch("/api/v1/users/me")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private ResultActions withdraw() throws Exception {
		return mockMvc.perform(delete("/api/v1/users/me").header("Authorization", "Bearer " + token));
	}

	private User reloadUser() {
		return userRepository.findById(user.getUserId()).orElseThrow();
	}

	// ---- 내 계정 조회 ----

	@Test
	void 내_계정을_조회한다() throws Exception {
		me().andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.userId").value(user.getUserId()))
				.andExpect(jsonPath("$.data.loginId").value("jjong123"))
				.andExpect(jsonPath("$.data.name").value("김쫑긋"))
				.andExpect(jsonPath("$.data.nickname").value("쫑긋이"))
				.andExpect(jsonPath("$.data.email").isEmpty())
				.andExpect(jsonPath("$.data.phone").isEmpty())
				.andExpect(jsonPath("$.data.role").value("USER"))
				.andExpect(jsonPath("$.data.status").value("ACTIVE"))
				.andExpect(jsonPath("$.data.createdAt").isNotEmpty())
				.andExpect(jsonPath("$.data.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.data.password").doesNotExist());
	}

	@Test
	void 조회는_토큰이_없으면_401() throws Exception {
		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}

	// ---- 내 계정 수정 ----

	@Test
	void 이름_닉네임_연락처_이메일을_수정한다() throws Exception {
		update("""
				{"name":"박쫑긋","nickname":"새닉네임","phone":"010-1234-5678","email":"jjong@example.com"}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("박쫑긋"))
				.andExpect(jsonPath("$.data.nickname").value("새닉네임"))
				.andExpect(jsonPath("$.data.phone").value("010-1234-5678"))
				.andExpect(jsonPath("$.data.email").value("jjong@example.com"));

		User saved = reloadUser();
		assertThat(saved.getName()).isEqualTo("박쫑긋");
		assertThat(saved.getNickname()).isEqualTo("새닉네임");
		assertThat(saved.getPhone()).isEqualTo("010-1234-5678");
		assertThat(saved.getEmail()).isEqualTo("jjong@example.com");
	}

	@Test
	void 보낸_항목만_바뀌고_나머지는_유지된다() throws Exception {
		update("""
				{"phone":"01012345678"}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.phone").value("01012345678"))
				.andExpect(jsonPath("$.data.name").value("김쫑긋"))
				.andExpect(jsonPath("$.data.nickname").value("쫑긋이"));
	}

	@Test
	void 연락처와_이메일은_빈_문자열로_지운다() throws Exception {
		update("""
				{"phone":"010-1234-5678","email":"jjong@example.com"}
				""").andExpect(status().isOk());

		update("""
				{"phone":"","email":""}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.phone").isEmpty())
				.andExpect(jsonPath("$.data.email").isEmpty());

		assertThat(reloadUser().getPhone()).isNull();
		assertThat(reloadUser().getEmail()).isNull();
	}

	@Test
	void 내_닉네임을_그대로_보내도_성공한다() throws Exception {
		update("""
				{"nickname":"쫑긋이"}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.nickname").value("쫑긋이"));
	}

	@Test
	void 다른_사용자가_쓰는_닉네임은_409() throws Exception {
		update("""
				{"nickname":"다른이"}
				""")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("AUTH_409_2"));

		assertThat(reloadUser().getNickname()).isEqualTo("쫑긋이");
	}

	@Test
	void 수정할_항목이_하나도_없으면_400() throws Exception {
		update("{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 형식이_틀리면_400() throws Exception {
		update("""
				{"name":"a"}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		update("""
				{"nickname":"닉 네임"}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		update("""
				{"nickname":""}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		update("""
				{"phone":"12345"}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		update("""
				{"email":"not-an-email"}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 수정은_본문이_깨졌으면_400() throws Exception {
		update("{").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 수정은_토큰이_없으면_401() throws Exception {
		mockMvc.perform(patch("/api/v1/users/me")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"박쫑긋"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}

	// ---- 회원 탈퇴 ----

	@Test
	void 탈퇴하면_계정이_탈퇴_상태가_되고_개인정보가_지워진다() throws Exception {
		update("""
				{"phone":"010-1234-5678","email":"jjong@example.com"}
				""").andExpect(status().isOk());

		withdraw().andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true));

		User saved = reloadUser();
		assertThat(saved.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
		assertThat(saved.getDeletedAt()).isNotNull();
		assertThat(saved.getLoginId()).isEqualTo("withdrawn_" + user.getUserId());
		assertThat(saved.getName()).isEqualTo("탈퇴한 사용자");
		assertThat(saved.getNickname()).isNull();
		assertThat(saved.getPhone()).isNull();
		assertThat(saved.getEmail()).isNull();
		assertThat(passwordEncoder.matches("password1", saved.getPasswordHash())).isFalse();
	}

	@Test
	void 탈퇴하면_같은_아이디로_로그인할_수_없다() throws Exception {
		withdraw().andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"loginId":"jjong123","password":"password1"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_401_1"));
	}

	@Test
	void 탈퇴하면_아이디와_닉네임을_다시_쓸_수_있다() throws Exception {
		withdraw().andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"새사람","nickname":"쫑긋이","loginId":"jjong123","password":"password1"}
								"""))
				.andExpect(status().isOk());
	}

	@Test
	void 탈퇴하면_로그인_세션과_사용자_데이터가_삭제된다() throws Exception {
		mockMvc.perform(patch("/api/v1/users/me/profile")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"maxDistanceKm":10}
								"""))
				.andExpect(status().isOk());
		experienceRepository.save(VolunteerExperience.create(user.getUserId(), List.of(ActivityCategory.WALK),
				"행복보호소", "2025.07", "산책 봉사를 했습니다.", null));
		RecommendationBatch batch = batchRepository
				.save(RecommendationBatch.create(user.getUserId(), RecommendationSource.RULE));
		itemRepository.save(RecommendationItem.create(batch.getRecommendationBatchId(), 12L, 1, 0.5, "이유",
				List.of("거리 가까움")));

		assertThat(refreshTokenRepository.findByUserId(user.getUserId())).isPresent();
		assertThat(profileRepository.findByUserId(user.getUserId())).isPresent();

		withdraw().andExpect(status().isOk());

		assertThat(refreshTokenRepository.findByUserId(user.getUserId())).isEmpty();
		assertThat(profileRepository.findByUserId(user.getUserId())).isEmpty();
		assertThat(experienceRepository.findAll()).noneMatch(e -> e.getUserId().equals(user.getUserId()));
		assertThat(batchRepository.findAll()).noneMatch(b -> b.getUserId().equals(user.getUserId()));
		assertThat(itemRepository.findAll()).noneMatch(i -> i.getRecommendationBatchId()
				.equals(batch.getRecommendationBatchId()));
	}

	@Test
	void 탈퇴해도_동의_이력과_행동_이벤트는_남는다() throws Exception {
		mockMvc.perform(post("/api/v1/users/me/consents")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"consents":[{"consentType":"TERMS","policyVersion":"v1","agreed":true}]}
								"""))
				.andExpect(status().isOk());
		eventRepository.save(UserActivityEvent.create(user.getUserId(), ActivityEventType.POSTING_VIEW, 12L, null,
				null, LocalDateTime.now()));

		withdraw().andExpect(status().isOk());

		assertThat(consentRepository.countByUserId(user.getUserId())).isEqualTo(1);
		assertThat(eventRepository.findAll()).hasSize(1);
	}

	@Test
	void 다른_사용자의_데이터는_탈퇴해도_그대로다() throws Exception {
		User other = userRepository.findByLoginId("other456").orElseThrow();
		RecommendationBatch otherBatch = batchRepository
				.save(RecommendationBatch.create(other.getUserId(), RecommendationSource.RULE));
		itemRepository.save(RecommendationItem.create(otherBatch.getRecommendationBatchId(), 12L, 1, 0.5, "이유",
				List.of("거리 가까움")));

		withdraw().andExpect(status().isOk());

		assertThat(batchRepository.findAll()).anyMatch(b -> b.getUserId().equals(other.getUserId()));
		assertThat(itemRepository.findAll()).anyMatch(i -> i.getRecommendationBatchId()
				.equals(otherBatch.getRecommendationBatchId()));
		assertThat(userRepository.findByLoginId("other456").orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void 탈퇴한_계정으로_조회_수정_재탈퇴는_403() throws Exception {
		withdraw().andExpect(status().isOk());

		me().andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("AUTH_403_1"));
		update("""
				{"name":"박쫑긋"}
				""").andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("AUTH_403_1"));
		withdraw().andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("AUTH_403_1"));
	}

	@Test
	void 탈퇴는_토큰이_없으면_401() throws Exception {
		mockMvc.perform(delete("/api/v1/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}
}

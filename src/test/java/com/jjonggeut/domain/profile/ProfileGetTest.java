package com.jjonggeut.domain.profile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjonggeut.domain.profile.entity.ActivityFrequency;
import com.jjonggeut.domain.profile.entity.AvailableDay;
import com.jjonggeut.domain.profile.entity.AvailableTimeSlot;
import com.jjonggeut.domain.profile.entity.ExperienceLevel;
import com.jjonggeut.domain.profile.entity.InterestAnimal;
import com.jjonggeut.domain.profile.entity.UserProfile;
import com.jjonggeut.domain.profile.repository.UserProfileRepository;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.repository.UserRepository;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProfileGetTest {

	private static final String URL = "/api/v1/users/me/profile";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserProfileRepository profileRepository;

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

	private UserProfile profileOf(Long userId, String sigungu) {
		return UserProfile.create(userId, "서울특별시", sigungu, 37.5894, 127.0167, 10,
				ExperienceLevel.ONE_TO_THREE, ActivityFrequency.MONTHLY_2_3,
				List.of(InterestAnimal.DOG, InterestAnimal.CAT),
				List.of(ActivityCategory.WALK, ActivityCategory.CLEANING),
				List.of(AvailableDay.SAT, AvailableDay.SUN),
				List.of(AvailableTimeSlot.MORNING, AvailableTimeSlot.AFTERNOON));
	}

	@Test
	void 저장된_프로필을_조회한다() throws Exception {
		profileRepository.saveAndFlush(profileOf(user.getUserId(), "성북구"));

		mockMvc.perform(get(URL).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.onboardingCompleted").value(true))
				.andExpect(jsonPath("$.data.region.sido").value("서울특별시"))
				.andExpect(jsonPath("$.data.region.sigungu").value("성북구"))
				.andExpect(jsonPath("$.data.region.latitude").value(37.5894))
				.andExpect(jsonPath("$.data.maxDistanceKm").value(10))
				.andExpect(jsonPath("$.data.experienceLevel").value("ONE_TO_THREE"))
				.andExpect(jsonPath("$.data.activityFrequency").value("MONTHLY_2_3"))
				.andExpect(jsonPath("$.data.interestAnimals[0]").value("DOG"))
				.andExpect(jsonPath("$.data.interestAnimals[1]").value("CAT"))
				.andExpect(jsonPath("$.data.interestCategories[0]").value("WALK"))
				.andExpect(jsonPath("$.data.availableDays[1]").value("SUN"))
				.andExpect(jsonPath("$.data.availableTimeSlots[0]").value("MORNING"));
	}

	@Test
	void 프로필이_없으면_200으로_빈_프로필을_응답한다() throws Exception {
		mockMvc.perform(get(URL).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.onboardingCompleted").value(false))
				.andExpect(jsonPath("$.data.region").isEmpty())
				.andExpect(jsonPath("$.data.maxDistanceKm").isEmpty())
				.andExpect(jsonPath("$.data.experienceLevel").isEmpty())
				.andExpect(jsonPath("$.data.interestAnimals").isEmpty())
				.andExpect(jsonPath("$.data.availableDays").isEmpty());
	}

	@Test
	void 지역을_입력하지_않았으면_region은_null이다() throws Exception {
		profileRepository.saveAndFlush(UserProfile.create(user.getUserId(), null, null, null, null, null,
				null, null, List.of(), List.of(ActivityCategory.WALK), List.of(), List.of()));

		mockMvc.perform(get(URL).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.onboardingCompleted").value(true))
				.andExpect(jsonPath("$.data.region").isEmpty())
				.andExpect(jsonPath("$.data.interestCategories[0]").value("WALK"));
	}

	@Test
	void 다른_사용자의_프로필은_보이지_않는다() throws Exception {
		profileRepository.saveAndFlush(profileOf(other.getUserId(), "강남구"));

		mockMvc.perform(get(URL).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.onboardingCompleted").value(false))
				.andExpect(jsonPath("$.data.region").isEmpty());
	}

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(get(URL))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}

	@Test
	void 잘못된_토큰이면_401() throws Exception {
		mockMvc.perform(get(URL).header("Authorization", "Bearer invalid.token.value"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}
}

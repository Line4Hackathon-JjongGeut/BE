package com.jjonggeut.domain.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjonggeut.domain.profile.repository.UserProfileRepository;
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
class ProfileUpdateTest {

	private static final String URL = "/api/v1/users/me/profile";

	private static final String FULL_BODY = """
			{
			  "region": {"sido": "서울특별시", "sigungu": "성북구", "latitude": 37.5894, "longitude": 127.0167},
			  "maxDistanceKm": 10,
			  "experienceLevel": "ONE_TO_THREE",
			  "activityFrequency": "MONTHLY_2_3",
			  "interestAnimals": ["DOG", "CAT"],
			  "interestCategories": ["WALK", "CLEANING"],
			  "availableDays": ["SAT", "SUN"],
			  "availableTimeSlots": ["MORNING", "AFTERNOON"]
			}
			""";

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

	private String token;
	private String otherToken;

	@BeforeEach
	void setUp() throws Exception {
		userRepository.save(User.createUser("jjong123", passwordEncoder.encode("password1"), "김쫑긋", "쫑긋이"));
		userRepository.save(User.createUser("other456", passwordEncoder.encode("password1"), "이다른", "다른이"));
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

	private ResultActions update(String bearer, String body) throws Exception {
		return mockMvc.perform(patch(URL)
				.header("Authorization", "Bearer " + bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private ResultActions getProfile(String bearer) throws Exception {
		return mockMvc.perform(get(URL).header("Authorization", "Bearer " + bearer));
	}

	@Test
	void 프로필이_없을_때_처음_저장하면_생성되고_응답한다() throws Exception {
		update(token, FULL_BODY)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.onboardingCompleted").value(true))
				.andExpect(jsonPath("$.data.region.sido").value("서울특별시"))
				.andExpect(jsonPath("$.data.region.sigungu").value("성북구"))
				.andExpect(jsonPath("$.data.maxDistanceKm").value(10))
				.andExpect(jsonPath("$.data.experienceLevel").value("ONE_TO_THREE"))
				.andExpect(jsonPath("$.data.interestAnimals[1]").value("CAT"))
				.andExpect(jsonPath("$.data.availableTimeSlots[0]").value("MORNING"));

		getProfile(token)
				.andExpect(jsonPath("$.data.onboardingCompleted").value(true))
				.andExpect(jsonPath("$.data.region.sigungu").value("성북구"))
				.andExpect(jsonPath("$.data.interestCategories[0]").value("WALK"));
		assertThat(profileRepository.count()).isEqualTo(1);
	}

	@Test
	void 일부만_보내면_나머지는_그대로_유지된다() throws Exception {
		update(token, FULL_BODY).andExpect(status().isOk());

		update(token, """
				{"maxDistanceKm": 20, "experienceLevel": "FOUR_PLUS"}
				""").andExpect(status().isOk());

		getProfile(token)
				.andExpect(jsonPath("$.data.maxDistanceKm").value(20))
				.andExpect(jsonPath("$.data.experienceLevel").value("FOUR_PLUS"))
				.andExpect(jsonPath("$.data.activityFrequency").value("MONTHLY_2_3"))
				.andExpect(jsonPath("$.data.region.sigungu").value("성북구"))
				.andExpect(jsonPath("$.data.interestAnimals[0]").value("DOG"))
				.andExpect(jsonPath("$.data.availableDays[1]").value("SUN"));
		assertThat(profileRepository.count()).isEqualTo(1);
	}

	@Test
	void 수정은_한_사용자에_대해_행을_하나만_유지한다() throws Exception {
		update(token, FULL_BODY).andExpect(status().isOk());
		update(token, FULL_BODY).andExpect(status().isOk());
		update(token, """
				{"availableDays": ["MON"]}
				""").andExpect(status().isOk());

		assertThat(profileRepository.count()).isEqualTo(1);
	}

	@Test
	void 빈_배열을_보내면_해당_목록이_비워진다() throws Exception {
		update(token, FULL_BODY).andExpect(status().isOk());

		update(token, """
				{"interestAnimals": [], "availableDays": []}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.interestAnimals").isEmpty())
				.andExpect(jsonPath("$.data.availableDays").isEmpty())
				.andExpect(jsonPath("$.data.interestCategories[0]").value("WALK"));
	}

	@Test
	void 지역을_보내면_통째로_교체된다() throws Exception {
		update(token, FULL_BODY).andExpect(status().isOk());

		update(token, """
				{"region": {"sido": "경기도", "sigungu": "수원시"}}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.region.sido").value("경기도"))
				.andExpect(jsonPath("$.data.region.sigungu").value("수원시"))
				.andExpect(jsonPath("$.data.region.latitude").isEmpty())
				.andExpect(jsonPath("$.data.region.longitude").isEmpty());
	}

	@Test
	void 중복된_값은_제거하고_순서는_유지한다() throws Exception {
		update(token, """
				{"interestCategories": ["CLEANING", "WALK", "CLEANING"]}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.interestCategories.length()").value(2))
				.andExpect(jsonPath("$.data.interestCategories[0]").value("CLEANING"))
				.andExpect(jsonPath("$.data.interestCategories[1]").value("WALK"));
	}

	@Test
	void 다른_사용자의_프로필에는_영향이_없다() throws Exception {
		update(token, FULL_BODY).andExpect(status().isOk());

		getProfile(otherToken)
				.andExpect(jsonPath("$.data.onboardingCompleted").value(false));
		update(otherToken, """
				{"maxDistanceKm": 5}
				""").andExpect(status().isOk());

		getProfile(token).andExpect(jsonPath("$.data.maxDistanceKm").value(10));
		getProfile(otherToken).andExpect(jsonPath("$.data.maxDistanceKm").value(5));
		assertThat(profileRepository.count()).isEqualTo(2);
	}

	@Test
	void 수정할_항목이_하나도_없으면_400() throws Exception {
		update(token, "{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		assertThat(profileRepository.count()).isZero();
	}

	@Test
	void 거리가_범위를_벗어나면_400() throws Exception {
		update(token, """
				{"maxDistanceKm": 0}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		update(token, """
				{"maxDistanceKm": 201}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 지역에_시군구가_없으면_400() throws Exception {
		update(token, """
				{"region": {"sido": "서울특별시"}}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 위도만_보내면_400() throws Exception {
		update(token, """
				{"region": {"sido": "서울특별시", "sigungu": "성북구", "latitude": 37.5}}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 위도_경도가_범위를_벗어나면_400() throws Exception {
		update(token, """
				{"region": {"sido": "서울특별시", "sigungu": "성북구", "latitude": 91, "longitude": 127.0}}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 정의되지_않은_값을_보내면_400() throws Exception {
		update(token, """
				{"experienceLevel": "EXPERT"}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.isSuccess").value(false));
		update(token, """
				{"interestCategories": ["FLYING"]}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.isSuccess").value(false));
	}

	@Test
	void 검증에_실패하면_저장되지_않는다() throws Exception {
		update(token, """
				{"region": {"sido": "서울특별시"}, "maxDistanceKm": 10}
				""").andExpect(status().isBadRequest());

		assertThat(profileRepository.count()).isZero();
	}

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON).content(FULL_BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}
}

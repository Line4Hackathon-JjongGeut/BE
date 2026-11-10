package com.jjonggeut.domain.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjonggeut.domain.consent.entity.ConsentType;
import com.jjonggeut.domain.consent.entity.UserConsent;
import com.jjonggeut.domain.consent.repository.UserConsentRepository;
import com.jjonggeut.domain.profile.entity.ActivityFrequency;
import com.jjonggeut.domain.profile.entity.AvailableDay;
import com.jjonggeut.domain.profile.entity.AvailableTimeSlot;
import com.jjonggeut.domain.profile.entity.ExperienceLevel;
import com.jjonggeut.domain.profile.entity.InterestAnimal;
import com.jjonggeut.domain.profile.entity.UserProfile;
import com.jjonggeut.domain.profile.repository.UserProfileRepository;
import com.jjonggeut.domain.recommendation.entity.PostingRef;
import com.jjonggeut.domain.recommendation.entity.RecommendationItem;
import com.jjonggeut.domain.recommendation.entity.ShelterRef;
import com.jjonggeut.domain.recommendation.repository.PostingRefRepository;
import com.jjonggeut.domain.recommendation.repository.RecommendationBatchRepository;
import com.jjonggeut.domain.recommendation.repository.RecommendationItemRepository;
import com.jjonggeut.domain.recommendation.repository.ShelterRefRepository;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.repository.UserRepository;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import com.jjonggeut.domain.volunteerexperience.entity.VolunteerExperience;
import com.jjonggeut.domain.volunteerexperience.repository.VolunteerExperienceRepository;
import com.jjonggeut.global.ai.AiClient;
import com.jjonggeut.global.ai.AiException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecommendationTest {

	private static final String URL = "/api/v1/recommendations";

	// 사용자 위치(성북구) 기준 약 1.2km / 약 28km 떨어진 시설
	private static final double HOME_LAT = 37.5894;
	private static final double HOME_LON = 127.0167;
	private static final double NEAR_LAT = 37.6000;
	private static final double NEAR_LON = 127.0200;
	private static final double FAR_LAT = 37.4563;
	private static final double FAR_LON = 126.7052;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserProfileRepository profileRepository;

	@Autowired
	private VolunteerExperienceRepository experienceRepository;

	@Autowired
	private UserConsentRepository consentRepository;

	@Autowired
	private PostingRefRepository postingRepository;

	@Autowired
	private ShelterRefRepository shelterRepository;

	@Autowired
	private RecommendationBatchRepository batchRepository;

	@Autowired
	private RecommendationItemRepository itemRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AiClient aiClient;

	private User user;
	private String token;

	@BeforeEach
	void setUp() throws Exception {
		user = userRepository.save(User.createUser("jjong123", passwordEncoder.encode("password1"), "김쫑긋", "쫑긋이"));
		token = login("jjong123");
		shelterRepository.save(ShelterRef.of(1L, "행복보호소", "서울특별시", "성북구", NEAR_LAT, NEAR_LON));
		shelterRepository.save(ShelterRef.of(2L, "먼보호소", "인천광역시", "남동구", FAR_LAT, FAR_LON));
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

	private ResultActions recommend(String query) throws Exception {
		return mockMvc.perform(get(URL + query).header("Authorization", "Bearer " + token));
	}

	private void posting(long id, long shelterId, String title, List<ActivityCategory> categories, String animal,
			Integer capacity, Integer applicants) {
		postingRepository.save(PostingRef.of(id, shelterId, "VOLUNTEER", title, "OPEN", categories, animal,
				LocalDateTime.now().plusDays(3).withHour(10).withMinute(0), capacity, applicants));
	}

	private void profile(List<ActivityCategory> interests, Integer maxDistanceKm, Double lat, Double lon) {
		profileRepository.save(UserProfile.create(user.getUserId(), "서울특별시", "성북구", lat, lon, maxDistanceKm,
				ExperienceLevel.FIRST, ActivityFrequency.MONTHLY_1, List.of(InterestAnimal.DOG), interests,
				List.of(), List.of()));
	}

	private void agreeAi(boolean agreed) {
		consentRepository.save(UserConsent.create(user.getUserId(), ConsentType.AI_PROCESSING, "2026-11-01", agreed));
	}

	@Test
	void 프로필_조건에_맞는_공고가_먼저_추천되고_거리_제한을_넘는_공고는_제외된다() throws Exception {
		profile(List.of(ActivityCategory.WALK), 10, HOME_LAT, HOME_LON);
		posting(1, 1, "주말 산책 봉사", List.of(ActivityCategory.WALK), "DOG", 10, 2);
		posting(2, 1, "목욕 미용 봉사", List.of(ActivityCategory.GROOMING), "DOG", 10, 2);
		posting(3, 2, "먼 곳 산책 봉사", List.of(ActivityCategory.WALK), "DOG", 10, 2);

		recommend("")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.source").value("RULE"))
				.andExpect(jsonPath("$.data.recommendationBatchId").isNumber())
				.andExpect(jsonPath("$.data.generatedAt").isNotEmpty())
				.andExpect(jsonPath("$.data.items.length()").value(2))
				.andExpect(jsonPath("$.data.items[0].rank").value(1))
				.andExpect(jsonPath("$.data.items[0].recommendationItemId").isNumber())
				.andExpect(jsonPath("$.data.items[0].posting.postingId").value(1))
				.andExpect(jsonPath("$.data.items[0].posting.title").value("주말 산책 봉사"))
				.andExpect(jsonPath("$.data.items[0].posting.shelterName").value("행복보호소"))
				.andExpect(jsonPath("$.data.items[0].posting.region.sigungu").value("성북구"))
				.andExpect(jsonPath("$.data.items[0].posting.distanceKm").isNumber())
				.andExpect(jsonPath("$.data.items[0].posting.activityCategories[0]").value("WALK"))
				.andExpect(jsonPath("$.data.items[0].reasonTags[?(@ == '관심 활동 일치')]").exists())
				.andExpect(jsonPath("$.data.items[0].reasonTags[?(@ == '거리 가까움')]").exists())
				.andExpect(jsonPath("$.data.items[0].reason").isNotEmpty())
				.andExpect(jsonPath("$.data.items[1].posting.postingId").value(2));

		verify(aiClient, never()).generateJson(anyString(), anyString(), anyMap());
	}

	@Test
	void 일정이_맞는_공고는_일정_일치_태그가_붙는다() throws Exception {
		LocalDateTime start = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0);
		AvailableDay day = AvailableDay.valueOf(start.getDayOfWeek().name().substring(0, 3));
		profileRepository.save(UserProfile.create(user.getUserId(), "서울특별시", "성북구", HOME_LAT, HOME_LON, 10,
				ExperienceLevel.FIRST, ActivityFrequency.MONTHLY_1, List.of(), List.of(),
				List.of(day), List.of(AvailableTimeSlot.MORNING)));
		posting(1, 1, "오전 봉사", List.of(ActivityCategory.WALK), null, 10, 2);

		recommend("")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].reasonTags[?(@ == '일정 일치')]").exists());
	}

	@Test
	void 봉사_경험과_같은_활동은_경험_연계로_반영된다() throws Exception {
		profile(List.of(), 10, HOME_LAT, HOME_LON);
		experienceRepository.save(VolunteerExperience.create(user.getUserId(), List.of(ActivityCategory.FEEDING),
				null, null, "급식 봉사를 했어요.", null));
		posting(1, 1, "배식 봉사", List.of(ActivityCategory.FEEDING), null, 10, 2);
		posting(2, 1, "산책 봉사", List.of(ActivityCategory.WALK), null, 10, 2);

		recommend("")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].posting.postingId").value(1))
				.andExpect(jsonPath("$.data.items[0].reasonTags[?(@ == '봉사 경험 연계')]").exists());
	}

	@Test
	void 신청이_적은_시설의_공고에_수요_태그가_붙는다() throws Exception {
		posting(1, 1, "도움이 필요한 시설", List.of(ActivityCategory.WALK), null, 10, 1);
		posting(2, 1, "신청이 가득 찬 시설", List.of(ActivityCategory.WALK), null, 10, 10);

		recommend("")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].posting.postingId").value(1))
				.andExpect(jsonPath("$.data.items[0].reasonTags[?(@ == '도움이 더 필요한 시설')]").exists())
				.andExpect(jsonPath("$.data.items[1].reasonTags[?(@ == '도움이 더 필요한 시설')]").doesNotExist());
	}

	@Test
	void AI_동의가_있으면_AI_이유를_사용하고_개인정보는_보내지_않는다() throws Exception {
		profile(List.of(ActivityCategory.WALK), 10, HOME_LAT, HOME_LON);
		posting(1, 1, "주말 산책 봉사", List.of(ActivityCategory.WALK), "DOG", 10, 2);
		posting(2, 1, "목욕 미용 봉사", List.of(ActivityCategory.GROOMING), "DOG", 10, 2);
		agreeAi(true);
		when(aiClient.generateJson(anyString(), anyString(), anyMap())).thenReturn("""
				{"reasons":[
				  {"postingId":1,"reason":"집에서 가까운 산책 봉사예요."},
				  {"postingId":999,"reason":"없는 공고의 이유"}
				]}
				""");

		recommend("")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.source").value("AI"))
				.andExpect(jsonPath("$.data.items[0].reason").value("집에서 가까운 산책 봉사예요."))
				// AI 가 이유를 주지 않은 공고는 규칙 기반 이유로 채운다.
				.andExpect(jsonPath("$.data.items[1].posting.postingId").value(2))
				.andExpect(jsonPath("$.data.items[1].reason").isNotEmpty());

		ArgumentCaptor<String> userText = ArgumentCaptor.forClass(String.class);
		verify(aiClient).generateJson(anyString(), userText.capture(), anyMap());
		assertThat(userText.getValue())
				.contains("주말 산책 봉사", "행복보호소")
				.doesNotContain("김쫑긋", "jjong123", "쫑긋이");
	}

	@Test
	void 동의를_철회했으면_AI를_쓰지_않는다() throws Exception {
		posting(1, 1, "주말 산책 봉사", List.of(ActivityCategory.WALK), null, 10, 2);
		agreeAi(true);
		agreeAi(false);

		recommend("")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.source").value("RULE"));

		verify(aiClient, never()).generateJson(anyString(), anyString(), anyMap());
	}

	@Test
	void AI_호출이_실패하면_규칙_기반으로_대체하고_200이다() throws Exception {
		posting(1, 1, "주말 산책 봉사", List.of(ActivityCategory.WALK), null, 10, 2);
		agreeAi(true);
		when(aiClient.generateJson(anyString(), anyString(), anyMap())).thenThrow(new AiException("AI 호출에 실패했습니다."));

		recommend("")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.source").value("RULE"))
				.andExpect(jsonPath("$.data.items[0].reason").isNotEmpty());
	}

	@Test
	void AI_응답이_JSON이_아니거나_쓸_수_없으면_규칙_기반으로_대체한다() throws Exception {
		posting(1, 1, "주말 산책 봉사", List.of(ActivityCategory.WALK), null, 10, 2);
		agreeAi(true);
		when(aiClient.generateJson(anyString(), anyString(), anyMap())).thenReturn("이건 JSON이 아니에요");

		recommend("")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.source").value("RULE"));

		when(aiClient.generateJson(anyString(), anyString(), anyMap()))
				.thenReturn("{\"reasons\":[{\"postingId\":999,\"reason\":\"x\"}]}");

		recommend("")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.source").value("RULE"));
	}

	@Test
	void 프로필이_없어도_200으로_추천한다() throws Exception {
		posting(1, 1, "주말 산책 봉사", List.of(ActivityCategory.WALK), null, 10, 2);
		posting(2, 2, "먼 곳 산책 봉사", List.of(ActivityCategory.WALK), null, 10, 2);

		recommend("")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.source").value("RULE"))
				.andExpect(jsonPath("$.data.items.length()").value(2))
				.andExpect(jsonPath("$.data.items[0].posting.distanceKm").isEmpty());
	}

	@Test
	void 현재_위치를_보내면_프로필_위치보다_우선한다() throws Exception {
		// 프로필 위치는 인천(먼 곳), 최대 거리 10km → 서울 시설은 제외된다.
		profile(List.of(ActivityCategory.WALK), 10, FAR_LAT, FAR_LON);
		posting(1, 1, "주말 산책 봉사", List.of(ActivityCategory.WALK), null, 10, 2);

		recommend("")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isEmpty());

		recommend("?latitude=" + HOME_LAT + "&longitude=" + HOME_LON)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].posting.distanceKm").isNumber());
	}

	@Test
	void limit만큼만_추천한다() throws Exception {
		posting(1, 1, "봉사 1", List.of(ActivityCategory.WALK), null, 10, 2);
		posting(2, 1, "봉사 2", List.of(ActivityCategory.WALK), null, 10, 2);
		posting(3, 1, "봉사 3", List.of(ActivityCategory.WALK), null, 10, 2);

		recommend("?limit=2")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(2));
	}

	@Test
	void 추천할_공고가_없으면_빈_목록으로_200이고_저장하지_않는다() throws Exception {
		recommend("")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isEmpty())
				.andExpect(jsonPath("$.data.recommendationBatchId").isEmpty());

		assertThat(batchRepository.count()).isZero();
	}

	@Test
	void 마감_공고와_봉사가_아닌_공고와_지난_공고는_제외된다() throws Exception {
		LocalDateTime future = LocalDateTime.now().plusDays(3);
		postingRepository.save(PostingRef.of(1L, 1L, "VOLUNTEER", "모집 중", "OPEN",
				List.of(ActivityCategory.WALK), null, future, 10, 2));
		postingRepository.save(PostingRef.of(2L, 1L, "VOLUNTEER", "마감", "CLOSED",
				List.of(ActivityCategory.WALK), null, future, 10, 2));
		postingRepository.save(PostingRef.of(3L, 1L, "SPONSOR", "후원", "OPEN",
				List.of(ActivityCategory.WALK), null, future, 10, 2));
		postingRepository.save(PostingRef.of(4L, 1L, "VOLUNTEER", "지난 일정", "OPEN",
				List.of(ActivityCategory.WALK), null, LocalDateTime.now().minusDays(1), 10, 2));

		recommend("")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].posting.postingId").value(1));
	}

	@Test
	void 추천_결과가_배치와_항목으로_저장된다() throws Exception {
		profile(List.of(ActivityCategory.WALK), 10, HOME_LAT, HOME_LON);
		posting(1, 1, "주말 산책 봉사", List.of(ActivityCategory.WALK), "DOG", 10, 2);
		posting(2, 1, "목욕 미용 봉사", List.of(ActivityCategory.GROOMING), "DOG", 10, 2);

		String response = recommend("").andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		long batchId = objectMapper.readTree(response).get("data").get("recommendationBatchId").asLong();

		assertThat(batchRepository.count()).isEqualTo(1);
		List<RecommendationItem> items = itemRepository.findByRecommendationBatchIdOrderByRankNoAsc(batchId);
		assertThat(items).hasSize(2);
		assertThat(items.get(0).getPostingId()).isEqualTo(1L);
		assertThat(items.get(0).getRankNo()).isEqualTo(1);
		assertThat(items.get(0).getReasonTags()).contains("관심 활동 일치");
		assertThat(items.get(0).getImpressedAt()).isNull();
	}

	@Test
	void limit이_범위를_벗어나면_400() throws Exception {
		recommend("?limit=0").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		recommend("?limit=21").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 위도_경도가_잘못되면_400() throws Exception {
		recommend("?latitude=37.5").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		recommend("?latitude=91&longitude=127").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		recommend("?latitude=37.5&longitude=181").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 숫자가_아닌_값이면_400() throws Exception {
		recommend("?limit=abc").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(get(URL))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}
}

package com.jjonggeut.domain.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjonggeut.domain.recommendation.entity.ActivityEventType;
import com.jjonggeut.domain.recommendation.entity.RecommendationBatch;
import com.jjonggeut.domain.recommendation.entity.RecommendationItem;
import com.jjonggeut.domain.recommendation.entity.RecommendationSource;
import com.jjonggeut.domain.recommendation.entity.UserActivityEvent;
import com.jjonggeut.domain.recommendation.repository.RecommendationBatchRepository;
import com.jjonggeut.domain.recommendation.repository.RecommendationItemRepository;
import com.jjonggeut.domain.recommendation.repository.UserActivityEventRepository;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.repository.UserRepository;
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
class ActivityEventTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RecommendationBatchRepository batchRepository;

	@Autowired
	private RecommendationItemRepository itemRepository;

	@Autowired
	private UserActivityEventRepository eventRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	private User user;
	private String token;
	private String otherToken;
	private RecommendationItem item;

	@BeforeEach
	void setUp() throws Exception {
		user = userRepository.save(User.createUser("jjong123", passwordEncoder.encode("password1"), "김쫑긋", "쫑긋이"));
		userRepository.save(User.createUser("other456", passwordEncoder.encode("password1"), "이다른", "다른이"));
		token = login("jjong123");
		otherToken = login("other456");

		RecommendationBatch batch = batchRepository
				.save(RecommendationBatch.create(user.getUserId(), RecommendationSource.RULE));
		item = itemRepository.save(RecommendationItem.create(batch.getRecommendationBatchId(), 12L, 1, 0.5,
				"추천 이유", List.of("거리 가까움")));
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

	private ResultActions record(String bearer, String body) throws Exception {
		return mockMvc.perform(post("/api/v1/activity-events")
				.header("Authorization", "Bearer " + bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	@Test
	void 공고_조회_이벤트를_기록한다() throws Exception {
		record(token, """
				{"eventType":"POSTING_VIEW","postingId":12}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.activityEventId").isNotEmpty())
				.andExpect(jsonPath("$.data.eventType").value("POSTING_VIEW"))
				.andExpect(jsonPath("$.data.postingId").value(12))
				.andExpect(jsonPath("$.data.occurredAt").isNotEmpty());

		List<UserActivityEvent> saved = eventRepository.findAll();
		assertThat(saved).hasSize(1);
		assertThat(saved.get(0).getUserId()).isEqualTo(user.getUserId());
		assertThat(saved.get(0).getOccurredAt()).isNotNull();
	}

	@Test
	void 시설_조회는_시설_ID로_기록한다() throws Exception {
		record(token, """
				{"eventType":"SHELTER_VIEW","shelterId":3}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.shelterId").value(3))
				.andExpect(jsonPath("$.data.postingId").isEmpty());
	}

	@Test
	void 북마크와_신청과_후원_링크_클릭도_기록한다() throws Exception {
		record(token, """
				{"eventType":"BOOKMARK","postingId":12}
				""").andExpect(status().isOk());
		record(token, """
				{"eventType":"APPLY","postingId":12}
				""").andExpect(status().isOk());
		record(token, """
				{"eventType":"DONATION_LINK_CLICK","shelterId":3}
				""").andExpect(status().isOk());
		record(token, """
				{"eventType":"DONATION_LINK_CLICK","postingId":12}
				""").andExpect(status().isOk());

		assertThat(eventRepository.findAll()).extracting(UserActivityEvent::getEventType)
				.containsExactly(ActivityEventType.BOOKMARK, ActivityEventType.APPLY,
						ActivityEventType.DONATION_LINK_CLICK, ActivityEventType.DONATION_LINK_CLICK);
	}

	@Test
	void 같은_이벤트를_여러_번_보내면_모두_쌓인다() throws Exception {
		String body = """
				{"eventType":"POSTING_VIEW","postingId":12}
				""";
		record(token, body).andExpect(status().isOk());
		record(token, body).andExpect(status().isOk());

		assertThat(eventRepository.findAll()).hasSize(2);
	}

	@Test
	void 발생_시각을_보내면_그_시각으로_기록한다() throws Exception {
		record(token, """
				{"eventType":"POSTING_VIEW","postingId":12,"occurredAt":"2026-11-01T09:00:00"}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.occurredAt").value("2026-11-01T09:00:00"));
	}

	@Test
	void 미래_시각은_400() throws Exception {
		String future = LocalDateTime.now().plusHours(1).withNano(0).toString();
		record(token, """
				{"eventType":"POSTING_VIEW","postingId":12,"occurredAt":"%s"}
				""".formatted(future))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));

		assertThat(eventRepository.findAll()).isEmpty();
	}

	@Test
	void 추천_항목을_함께_보내면_전환_분석용으로_연결된다() throws Exception {
		record(token, """
				{"eventType":"APPLY","postingId":12,"recommendationItemId":%d}
				""".formatted(item.getRecommendationItemId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.recommendationItemId").value(item.getRecommendationItemId()));

		assertThat(eventRepository.findAll().get(0).getRecommendationItemId())
				.isEqualTo(item.getRecommendationItemId());
	}

	@Test
	void 추천_항목의_공고와_다른_공고_ID면_400() throws Exception {
		record(token, """
				{"eventType":"APPLY","postingId":99,"recommendationItemId":%d}
				""".formatted(item.getRecommendationItemId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));

		assertThat(eventRepository.findAll()).isEmpty();
	}

	@Test
	void 없는_추천_항목은_404() throws Exception {
		record(token, """
				{"eventType":"APPLY","postingId":12,"recommendationItemId":999999}
				""")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RECOMMENDATION_404_1"));
	}

	@Test
	void 다른_사용자의_추천_항목은_404이고_기록되지_않는다() throws Exception {
		record(otherToken, """
				{"eventType":"APPLY","postingId":12,"recommendationItemId":%d}
				""".formatted(item.getRecommendationItemId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RECOMMENDATION_404_1"));

		assertThat(eventRepository.findAll()).isEmpty();
	}

	@Test
	void 이벤트_종류가_없으면_400() throws Exception {
		record(token, """
				{"postingId":12}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 알_수_없는_이벤트_종류는_400() throws Exception {
		record(token, """
				{"eventType":"SHARE","postingId":12}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 이벤트에_맞는_대상_ID가_없으면_400() throws Exception {
		record(token, """
				{"eventType":"POSTING_VIEW","shelterId":3}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		record(token, """
				{"eventType":"SHELTER_VIEW","postingId":12}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		record(token, """
				{"eventType":"DONATION_LINK_CLICK"}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));

		assertThat(eventRepository.findAll()).isEmpty();
	}

	@Test
	void ID가_0이거나_음수면_400() throws Exception {
		record(token, """
				{"eventType":"POSTING_VIEW","postingId":0}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		record(token, """
				{"eventType":"POSTING_VIEW","postingId":12,"recommendationItemId":-1}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 본문이_깨졌거나_없으면_400() throws Exception {
		record(token, "{").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
		mockMvc.perform(post("/api/v1/activity-events")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(post("/api/v1/activity-events")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"eventType":"POSTING_VIEW","postingId":12}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}
}

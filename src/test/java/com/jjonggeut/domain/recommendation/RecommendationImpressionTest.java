package com.jjonggeut.domain.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjonggeut.domain.recommendation.entity.RecommendationBatch;
import com.jjonggeut.domain.recommendation.entity.RecommendationItem;
import com.jjonggeut.domain.recommendation.entity.RecommendationSource;
import com.jjonggeut.domain.recommendation.repository.RecommendationBatchRepository;
import com.jjonggeut.domain.recommendation.repository.RecommendationItemRepository;
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
class RecommendationImpressionTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RecommendationBatchRepository batchRepository;

	@Autowired
	private RecommendationItemRepository itemRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	private User user;
	private User other;
	private String token;
	private String otherToken;
	private RecommendationBatch batch;
	private RecommendationItem item1;
	private RecommendationItem item2;
	private RecommendationItem item3;

	@BeforeEach
	void setUp() throws Exception {
		user = userRepository.save(User.createUser("jjong123", passwordEncoder.encode("password1"), "김쫑긋", "쫑긋이"));
		other = userRepository.save(User.createUser("other456", passwordEncoder.encode("password1"), "이다른", "다른이"));
		token = login("jjong123");
		otherToken = login("other456");

		batch = batchRepository.save(RecommendationBatch.create(user.getUserId(), RecommendationSource.RULE));
		item1 = item(batch, 11L, 1);
		item2 = item(batch, 12L, 2);
		item3 = item(batch, 13L, 3);
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

	private RecommendationItem item(RecommendationBatch target, long postingId, int rank) {
		return itemRepository.save(RecommendationItem.create(target.getRecommendationBatchId(), postingId, rank, 0.5,
				"추천 이유", List.of("거리 가까움")));
	}

	private ResultActions impress(String bearer, long batchId, String body) throws Exception {
		var request = post("/api/v1/recommendations/" + batchId + "/impressions")
				.header("Authorization", "Bearer " + bearer);
		if (body != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(body);
		}
		return mockMvc.perform(request);
	}

	private RecommendationItem reload(RecommendationItem item) {
		return itemRepository.findById(item.getRecommendationItemId()).orElseThrow();
	}

	@Test
	void 항목을_지정하면_그_항목만_노출로_기록된다() throws Exception {
		impress(token, batch.getRecommendationBatchId(), """
				{"itemIds": [%d, %d]}
				""".formatted(item1.getRecommendationItemId(), item3.getRecommendationItemId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.recommendationBatchId").value(batch.getRecommendationBatchId()))
				.andExpect(jsonPath("$.data.recordedItemIds.length()").value(2))
				.andExpect(jsonPath("$.data.recordedItemIds[0]").value(item1.getRecommendationItemId()))
				.andExpect(jsonPath("$.data.recordedItemIds[1]").value(item3.getRecommendationItemId()))
				.andExpect(jsonPath("$.data.alreadyRecordedItemIds").isEmpty());

		assertThat(reload(item1).getImpressedAt()).isNotNull();
		assertThat(reload(item2).getImpressedAt()).isNull();
		assertThat(reload(item3).getImpressedAt()).isNotNull();
	}

	@Test
	void itemIds를_생략하면_묶음_전체가_기록된다() throws Exception {
		impress(token, batch.getRecommendationBatchId(), "{}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.recordedItemIds.length()").value(3))
				.andExpect(jsonPath("$.data.recordedItemIds[0]").value(item1.getRecommendationItemId()));

		assertThat(reload(item1).getImpressedAt()).isNotNull();
		assertThat(reload(item2).getImpressedAt()).isNotNull();
		assertThat(reload(item3).getImpressedAt()).isNotNull();
	}

	@Test
	void 본문_없이_호출해도_묶음_전체가_기록된다() throws Exception {
		impress(token, batch.getRecommendationBatchId(), null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.recordedItemIds.length()").value(3));
	}

	@Test
	void 다시_보내도_최초_노출_시각은_바뀌지_않는다() throws Exception {
		LocalDateTime first = LocalDateTime.of(2026, 11, 1, 9, 0);
		item1.markImpressed(first);
		itemRepository.saveAndFlush(item1);

		impress(token, batch.getRecommendationBatchId(), """
				{"itemIds": [%d, %d]}
				""".formatted(item1.getRecommendationItemId(), item2.getRecommendationItemId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.recordedItemIds.length()").value(1))
				.andExpect(jsonPath("$.data.recordedItemIds[0]").value(item2.getRecommendationItemId()))
				.andExpect(jsonPath("$.data.alreadyRecordedItemIds.length()").value(1))
				.andExpect(jsonPath("$.data.alreadyRecordedItemIds[0]").value(item1.getRecommendationItemId()));

		assertThat(reload(item1).getImpressedAt()).isEqualTo(first);
	}

	@Test
	void 같은_요청을_두_번_보내면_두_번째는_모두_이미_기록됨이다() throws Exception {
		impress(token, batch.getRecommendationBatchId(), "{}").andExpect(status().isOk());

		impress(token, batch.getRecommendationBatchId(), "{}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.recordedItemIds").isEmpty())
				.andExpect(jsonPath("$.data.alreadyRecordedItemIds.length()").value(3));
	}

	@Test
	void 묶음에_속하지_않는_항목이_있으면_400이고_아무것도_기록하지_않는다() throws Exception {
		RecommendationBatch another = batchRepository
				.save(RecommendationBatch.create(user.getUserId(), RecommendationSource.RULE));
		RecommendationItem foreign = item(another, 99L, 1);

		impress(token, batch.getRecommendationBatchId(), """
				{"itemIds": [%d, %d]}
				""".formatted(item1.getRecommendationItemId(), foreign.getRecommendationItemId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("RECOMMENDATION_400_1"));

		assertThat(reload(item1).getImpressedAt()).isNull();
		assertThat(reload(foreign).getImpressedAt()).isNull();
	}

	@Test
	void 존재하지_않는_항목_ID도_400() throws Exception {
		impress(token, batch.getRecommendationBatchId(), """
				{"itemIds": [999999]}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("RECOMMENDATION_400_1"));
	}

	@Test
	void 빈_배열이거나_20개를_넘으면_400() throws Exception {
		impress(token, batch.getRecommendationBatchId(), """
				{"itemIds": []}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));

		String many = java.util.stream.LongStream.rangeClosed(1, 21).boxed()
				.map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
		impress(token, batch.getRecommendationBatchId(), "{\"itemIds\": [" + many + "]}")
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 중복된_항목_ID는_400() throws Exception {
		impress(token, batch.getRecommendationBatchId(), """
				{"itemIds": [%d, %d]}
				""".formatted(item1.getRecommendationItemId(), item1.getRecommendationItemId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 없는_묶음은_404() throws Exception {
		impress(token, 999999L, "{}")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RECOMMENDATION_404_1"));
	}

	@Test
	void 다른_사용자의_묶음은_404이고_기록되지_않는다() throws Exception {
		impress(otherToken, batch.getRecommendationBatchId(), "{}")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RECOMMENDATION_404_1"));

		assertThat(reload(item1).getImpressedAt()).isNull();
		assertThat(other.getUserId()).isNotEqualTo(user.getUserId());
	}

	@Test
	void 묶음_ID가_숫자가_아니면_400() throws Exception {
		mockMvc.perform(post("/api/v1/recommendations/abc/impressions")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(post("/api/v1/recommendations/" + batch.getRecommendationBatchId() + "/impressions")
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}
}

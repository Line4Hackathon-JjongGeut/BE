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
class RecommendationClickTest {

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
	private String token;
	private String otherToken;
	private RecommendationBatch batch;
	private RecommendationItem item;

	@BeforeEach
	void setUp() throws Exception {
		user = userRepository.save(User.createUser("jjong123", passwordEncoder.encode("password1"), "김쫑긋", "쫑긋이"));
		userRepository.save(User.createUser("other456", passwordEncoder.encode("password1"), "이다른", "다른이"));
		token = login("jjong123");
		otherToken = login("other456");

		batch = batchRepository.save(RecommendationBatch.create(user.getUserId(), RecommendationSource.RULE));
		item = item(batch, 12L, 1);
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

	private ResultActions click(String bearer, Object batchId, Object itemId) throws Exception {
		return mockMvc.perform(post("/api/v1/recommendations/" + batchId + "/items/" + itemId + "/clicks")
				.header("Authorization", "Bearer " + bearer));
	}

	private RecommendationItem reload(RecommendationItem target) {
		return itemRepository.findById(target.getRecommendationItemId()).orElseThrow();
	}

	@Test
	void 처음_클릭하면_클릭_수_1과_최초_클릭_시각이_기록된다() throws Exception {
		click(token, batch.getRecommendationBatchId(), item.getRecommendationItemId())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.recommendationBatchId").value(batch.getRecommendationBatchId()))
				.andExpect(jsonPath("$.data.recommendationItemId").value(item.getRecommendationItemId()))
				.andExpect(jsonPath("$.data.postingId").value(12))
				.andExpect(jsonPath("$.data.clickCount").value(1))
				.andExpect(jsonPath("$.data.clickedAt").isNotEmpty());

		RecommendationItem saved = reload(item);
		assertThat(saved.getClickCount()).isEqualTo(1);
		assertThat(saved.getClickedAt()).isNotNull();
	}

	@Test
	void 다시_클릭하면_클릭_수만_늘고_최초_클릭_시각은_유지된다() throws Exception {
		LocalDateTime first = LocalDateTime.of(2026, 11, 1, 9, 0);
		item.recordClick(first);
		itemRepository.saveAndFlush(item);

		click(token, batch.getRecommendationBatchId(), item.getRecommendationItemId())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.clickCount").value(2));
		click(token, batch.getRecommendationBatchId(), item.getRecommendationItemId())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.clickCount").value(3))
				.andExpect(jsonPath("$.data.clickedAt").value("2026-11-01T09:00:00"));

		RecommendationItem saved = reload(item);
		assertThat(saved.getClickCount()).isEqualTo(3);
		assertThat(saved.getClickedAt()).isEqualTo(first);
	}

	@Test
	void 노출이_비어_있으면_클릭과_함께_노출도_기록된다() throws Exception {
		assertThat(item.getImpressedAt()).isNull();

		click(token, batch.getRecommendationBatchId(), item.getRecommendationItemId())
				.andExpect(status().isOk());

		assertThat(reload(item).getImpressedAt()).isNotNull();
	}

	@Test
	void 이미_기록된_노출_시각은_클릭해도_바뀌지_않는다() throws Exception {
		LocalDateTime shown = LocalDateTime.of(2026, 11, 1, 8, 0);
		item.markImpressed(shown);
		itemRepository.saveAndFlush(item);

		click(token, batch.getRecommendationBatchId(), item.getRecommendationItemId())
				.andExpect(status().isOk());

		assertThat(reload(item).getImpressedAt()).isEqualTo(shown);
	}

	@Test
	void 다른_항목의_클릭에는_영향이_없다() throws Exception {
		RecommendationItem another = item(batch, 13L, 2);

		click(token, batch.getRecommendationBatchId(), item.getRecommendationItemId())
				.andExpect(status().isOk());

		assertThat(reload(another).getClickCount()).isZero();
		assertThat(reload(another).getClickedAt()).isNull();
		assertThat(reload(another).getImpressedAt()).isNull();
	}

	@Test
	void 항목이_해당_묶음에_속하지_않으면_404() throws Exception {
		RecommendationBatch another = batchRepository
				.save(RecommendationBatch.create(user.getUserId(), RecommendationSource.RULE));
		RecommendationItem foreign = item(another, 99L, 1);

		click(token, batch.getRecommendationBatchId(), foreign.getRecommendationItemId())
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RECOMMENDATION_404_1"));

		assertThat(reload(foreign).getClickCount()).isZero();
	}

	@Test
	void 없는_묶음이나_항목은_404() throws Exception {
		click(token, 999999L, item.getRecommendationItemId())
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RECOMMENDATION_404_1"));
		click(token, batch.getRecommendationBatchId(), 999999L)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RECOMMENDATION_404_1"));
	}

	@Test
	void 다른_사용자의_추천_항목은_404이고_기록되지_않는다() throws Exception {
		click(otherToken, batch.getRecommendationBatchId(), item.getRecommendationItemId())
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RECOMMENDATION_404_1"));

		RecommendationItem saved = reload(item);
		assertThat(saved.getClickCount()).isZero();
		assertThat(saved.getClickedAt()).isNull();
		assertThat(saved.getImpressedAt()).isNull();
	}

	@Test
	void ID가_숫자가_아니면_400() throws Exception {
		click(token, "abc", item.getRecommendationItemId())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
		click(token, batch.getRecommendationBatchId(), "abc")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(post("/api/v1/recommendations/" + batch.getRecommendationBatchId() + "/items/"
						+ item.getRecommendationItemId() + "/clicks"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}
}

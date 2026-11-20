package com.jjonggeut.domain.posting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jjonggeut.domain.posting.entity.FosterContactMethod;
import com.jjonggeut.domain.posting.entity.FosterInquiry;
import com.jjonggeut.domain.posting.entity.FosterInquiryStatus;
import com.jjonggeut.domain.posting.repository.FosterInquiryRepository;
import com.jjonggeut.domain.recommendation.entity.ActivityEventType;
import com.jjonggeut.domain.recommendation.entity.RecommendationBatch;
import com.jjonggeut.domain.recommendation.entity.RecommendationItem;
import com.jjonggeut.domain.recommendation.entity.RecommendationSource;
import com.jjonggeut.domain.recommendation.repository.RecommendationBatchRepository;
import com.jjonggeut.domain.recommendation.repository.RecommendationItemRepository;
import com.jjonggeut.domain.recommendation.repository.UserActivityEventRepository;
import org.springframework.test.web.servlet.ResultActions;
import com.jjonggeut.domain.shelter.entity.VerificationStatus;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostingActionsTest extends PostingTestBase {

	@Autowired
	UserActivityEventRepository eventRepository;

	@Autowired
	RecommendationBatchRepository batchRepository;

	@Autowired
	RecommendationItemRepository itemRepository;

	@Autowired
	FosterInquiryRepository inquiryRepository;

	@BeforeEach
	void setUpData() {
		shelter(1, "성북 행복보호소", "서울특별시", "성북구", 37.5894, 127.0167, VerificationStatus.VERIFIED);
		posting(401, 1, "SPONSOR", "OPEN", "겨울 담요 후원", List.of(ActivityCategory.ETC), "ALL", null, null, null)
				.withSponsor("https://example.org/donate", "담요 구입");
		posting(402, 1, "SPONSOR", "OPEN", "링크 없는 후원", List.of(ActivityCategory.ETC), "ALL", null, null, null);
		posting(403, 1, "VOLUNTEER", "OPEN", "산책 봉사", List.of(ActivityCategory.WALK), "DOG", null, 5, 0);
		posting(404, 1, "FOSTER", "OPEN", "임시보호자 모집", List.of(ActivityCategory.FEEDING_CARE), "CAT", null, 1, 0)
				.withFoster(FosterContactMethod.APP, null);
		posting(405, 1, "FOSTER", "OPEN", "외부 문의 임시보호", List.of(ActivityCategory.FEEDING_CARE), "CAT", null, 1, 0)
				.withFoster(FosterContactMethod.EXTERNAL, "https://example.org/foster");
		posting(406, 1, "FOSTER", "CLOSED", "마감된 임시보호", List.of(ActivityCategory.FEEDING_CARE), "CAT", null, 1, 1)
				.withFoster(FosterContactMethod.APP, null);
		posting(407, 1, "SPONSOR", "OPEN", "비공개 후원", List.of(ActivityCategory.ETC), "ALL", null, null, null)
				.withSponsor("https://example.org/private", "비공개").withPublic(false);
		savePostings();
	}

	// ---- 후원 링크 이동 기록 ----

	private ResultActions donate(Object postingId, String body) throws Exception {
		return send(post("/api/v1/postings/" + postingId + "/donation-link-clicks"), body);
	}

	@Test
	void 본문_없이도_후원_링크_이동을_기록하고_링크를_돌려준다() throws Exception {
		donate(401, null).andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.postingId").value(401))
				.andExpect(jsonPath("$.data.donationUrl").value("https://example.org/donate"))
				.andExpect(jsonPath("$.data.recordedAt").isNotEmpty());

		var events = eventRepository.findAll();
		assertThat(events).hasSize(1);
		assertThat(events.get(0).getEventType()).isEqualTo(ActivityEventType.DONATION_LINK_CLICK);
		assertThat(events.get(0).getUserId()).isEqualTo(user.getUserId());
		assertThat(events.get(0).getPostingId()).isEqualTo(401L);
		assertThat(events.get(0).getShelterId()).isEqualTo(1L);
		assertThat(events.get(0).getRecommendationItemId()).isNull();
	}

	@Test
	void 내_추천에서_들어왔다면_추천_항목도_함께_기록한다() throws Exception {
		RecommendationBatch batch = batchRepository.save(RecommendationBatch.create(user.getUserId(),
				RecommendationSource.RULE));
		RecommendationItem item = itemRepository.save(RecommendationItem.create(batch.getRecommendationBatchId(),
				401L, 1, 0.9, "후원 추천", List.of("후원")));

		donate(401, "{\"recommendationItemId\":" + item.getRecommendationItemId() + "}")
				.andExpect(status().isOk());

		assertThat(eventRepository.findAll().get(0).getRecommendationItemId())
				.isEqualTo(item.getRecommendationItemId());
	}

	@Test
	void 없는_추천_항목이면_404() throws Exception {
		donate(401, "{\"recommendationItemId\":999999}").andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RECOMMENDATION_404_1"));
		assertThat(eventRepository.count()).isZero();
	}

	@Test
	void 다른_공고의_추천_항목이면_400() throws Exception {
		RecommendationBatch batch = batchRepository.save(RecommendationBatch.create(user.getUserId(),
				RecommendationSource.RULE));
		RecommendationItem item = itemRepository.save(RecommendationItem.create(batch.getRecommendationBatchId(),
				402L, 1, 0.9, "다른 공고", List.of("후원")));

		donate(401, "{\"recommendationItemId\":" + item.getRecommendationItemId() + "}")
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 추천_항목_ID가_양수가_아니면_400() throws Exception {
		donate(401, "{\"recommendationItemId\":-1}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		donate(401, "{\"recommendationItemId\":\"abc\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 후원_공고가_아니거나_링크가_없으면_POSTING_400_1() throws Exception {
		donate(402, null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("POSTING_400_1"));
		donate(403, null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("POSTING_400_1"));
		assertThat(eventRepository.count()).isZero();
	}

	@Test
	void 후원_링크는_없거나_비공개인_공고면_404이고_ID가_틀리면_400() throws Exception {
		donate(9999, null).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("POSTING_404_1"));
		donate(407, null).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("POSTING_404_1"));
		donate(0, null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		donate("abc", null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 후원_링크_기록은_토큰이_없으면_401() throws Exception {
		mockMvc.perform(post("/api/v1/postings/401/donation-link-clicks")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}

	// ---- 임시보호 문의 ----

	private ResultActions inquire(Object postingId, String body) throws Exception {
		return send(post("/api/v1/postings/" + postingId + "/foster-inquiries"), body);
	}

	@Test
	void 임시보호_참여_의사를_남긴다() throws Exception {
		inquire(404, "{\"message\":\"고양이를 돌봐본 적 있어요.\",\"contactPhone\":\"010-1234-5678\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.inquiryId").isNumber())
				.andExpect(jsonPath("$.data.postingId").value(404))
				.andExpect(jsonPath("$.data.status").value("RECEIVED"))
				.andExpect(jsonPath("$.data.createdAt").isNotEmpty());

		var saved = inquiryRepository.findAll();
		assertThat(saved).hasSize(1);
		assertThat(saved.get(0).getUserId()).isEqualTo(user.getUserId());
		assertThat(saved.get(0).getMessage()).isEqualTo("고양이를 돌봐본 적 있어요.");
		assertThat(saved.get(0).getContactPhone()).isEqualTo("010-1234-5678");
	}

	@Test
	void 연락처를_생략하면_내_계정_전화번호를_쓰고_말은_공백을_정리한다() throws Exception {
		user.changePhone("010-9999-8888");
		userRepository.save(user);

		inquire(404, "{\"message\":\"  잘 부탁드립니다  \"}").andExpect(status().isOk());

		var saved = inquiryRepository.findAll().get(0);
		assertThat(saved.getContactPhone()).isEqualTo("010-9999-8888");
		assertThat(saved.getMessage()).isEqualTo("잘 부탁드립니다");
	}

	@Test
	void 본문_없이도_접수할_수_있다() throws Exception {
		user.changePhone("010-9999-8888");
		userRepository.save(user);

		inquire(404, null).andExpect(status().isOk());
		assertThat(inquiryRepository.findAll().get(0).getMessage()).isNull();
	}

	@Test
	void 처리_전_문의가_있으면_409() throws Exception {
		inquire(404, "{\"contactPhone\":\"010-1234-5678\"}").andExpect(status().isOk());

		inquire(404, "{\"contactPhone\":\"010-1234-5678\"}").andExpect(status().isConflict())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("POSTING_409_3"));
		assertThat(inquiryRepository.count()).isEqualTo(1);
	}

	@Test
	void 확인된_문의가_있으면_다시_남길_수_있고_다른_사용자와는_독립이다() throws Exception {
		inquiryRepository.save(FosterInquiry.of(404L, user.getUserId(), null, "010-1111-2222",
				FosterInquiryStatus.CHECKED));
		inquiryRepository.save(FosterInquiry.of(404L, other.getUserId(), null, "010-3333-4444",
				FosterInquiryStatus.RECEIVED));

		inquire(404, "{\"contactPhone\":\"010-1234-5678\"}").andExpect(status().isOk());
	}

	@Test
	void 모집이_끝난_공고는_409() throws Exception {
		inquire(406, "{\"contactPhone\":\"010-1234-5678\"}").andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("POSTING_409_4"));
	}

	@Test
	void 앱으로_받는_임시보호_공고가_아니면_POSTING_400_1() throws Exception {
		inquire(405, "{\"contactPhone\":\"010-1234-5678\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("POSTING_400_1"));
		inquire(403, "{\"contactPhone\":\"010-1234-5678\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("POSTING_400_1"));
	}

	@Test
	void 연락처_형식이나_말_길이가_틀리면_400() throws Exception {
		inquire(404, "{\"contactPhone\":\"abc\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		inquire(404, "{\"message\":\"" + "가".repeat(501) + "\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		inquire(404, "{broken").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 없는_공고는_404이고_ID가_틀리면_400() throws Exception {
		inquire(9999, null).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("POSTING_404_1"));
		inquire(0, null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		inquire("abc", null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 임시보호_문의는_토큰이_없으면_401() throws Exception {
		mockMvc.perform(post("/api/v1/postings/404/foster-inquiries")).andExpect(status().isUnauthorized());
	}

	@Test
	void 회원_탈퇴하면_내_임시보호_문의는_지우고_다른_사용자_것은_남긴다() throws Exception {
		inquire(404, "{\"contactPhone\":\"010-1234-5678\"}").andExpect(status().isOk());
		inquiryRepository.save(FosterInquiry.of(404L, other.getUserId(), null, "010-3333-4444",
				FosterInquiryStatus.RECEIVED));

		send(delete("/api/v1/users/me"), null).andExpect(status().isOk());

		assertThat(inquiryRepository.findAll()).hasSize(1);
		assertThat(inquiryRepository.findAll().get(0).getUserId()).isEqualTo(other.getUserId());
	}
}

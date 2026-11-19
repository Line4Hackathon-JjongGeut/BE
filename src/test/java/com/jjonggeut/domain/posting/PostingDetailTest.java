package com.jjonggeut.domain.posting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jjonggeut.domain.activity.entity.PostingBookmarkRef;
import com.jjonggeut.domain.activity.repository.PostingBookmarkRefRepository;
import com.jjonggeut.domain.posting.entity.FosterContactMethod;
import com.jjonggeut.domain.posting.entity.PetGender;
import com.jjonggeut.domain.posting.entity.PostingApplicationQuestion;
import com.jjonggeut.domain.posting.entity.PostingSchedule;
import com.jjonggeut.domain.posting.entity.QuestionType;
import com.jjonggeut.domain.posting.repository.PostingApplicationQuestionRepository;
import com.jjonggeut.domain.posting.repository.PostingScheduleRepository;
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
class PostingDetailTest extends PostingTestBase {

	@Autowired
	PostingBookmarkRefRepository bookmarkRepository;

	@Autowired
	PostingScheduleRepository scheduleRepository;

	@Autowired
	PostingApplicationQuestionRepository questionRepository;

	@BeforeEach
	void setUpData() {
		shelter(1, "성북 행복보호소", "서울특별시", "성북구", 37.5894, 127.0167, VerificationStatus.VERIFIED);
		shelter(2, "노원 온기쉼터", "서울특별시", "노원구", 37.6542, 127.0568, VerificationStatus.UNVERIFIED);

		posting(201, 1, "VOLUNTEER", "OPEN", "주말 강아지 산책", List.of(ActivityCategory.WALK, ActivityCategory.FEEDING_CARE), "DOG",
				LocalDateTime.of(2026, 12, 10, 10, 0), 10, 3)
				.withDescription("보호소 강아지들과 산책합니다.", LocalDateTime.of(2026, 11, 1, 9, 0))
				.withUrgent(true, 80).withPeriod(LocalDateTime.of(2026, 12, 8, 18, 0), LocalDateTime.of(2026, 12, 10, 12, 0))
				.withConditions(false, 18, "산책 경험자");
		posting(202, 2, "SPONSOR", "OPEN", "겨울 담요 후원", List.of(ActivityCategory.ETC), "ALL", null, null, null)
				.withSponsor("https://example.org/donate", "담요 구입")
				.withDonationQuantity(30, 9);
		posting(203, 2, "SPONSOR", "OPEN", "링크 없는 후원", List.of(ActivityCategory.ETC), "ALL", null, null, null);
		posting(204, 1, "FOSTER", "OPEN", "임시보호자 모집", List.of(ActivityCategory.FEEDING_CARE), "CAT", null, 3, 0)
				.withFoster(FosterContactMethod.APP, null)
				.withPetInfo("나비", "약 6개월", PetGender.FEMALE, "사람을 잘 따름");
		posting(205, 1, "FOSTER", "OPEN", "외부 문의 임시보호", List.of(ActivityCategory.FEEDING_CARE), "CAT", null, 3, 0)
				.withFoster(FosterContactMethod.EXTERNAL, "https://example.org/foster");
		posting(206, 1, "VOLUNTEER", "OPEN", "비공개 공고", List.of(ActivityCategory.WALK), "DOG", null, 5, 0)
				.withPublic(false);
		posting(207, 1, "VOLUNTEER", "CLOSED", "마감 공고", List.of(ActivityCategory.WALK), "DOG",
				LocalDateTime.of(2026, 12, 1, 10, 0), 5, 5);
		savePostings();
	}

	// ---- 상세 ----

	@Test
	void 봉사_공고_상세를_보여_준다() throws Exception {
		get("/api/v1/postings/201").andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.postingId").value(201))
				.andExpect(jsonPath("$.data.postingType").value("VOLUNTEER"))
				.andExpect(jsonPath("$.data.title").value("주말 강아지 산책"))
				.andExpect(jsonPath("$.data.status").value("OPEN"))
				.andExpect(jsonPath("$.data.urgent").value(true))
				.andExpect(jsonPath("$.data.description").value("보호소 강아지들과 산책합니다."))
				.andExpect(jsonPath("$.data.animalType").value("DOG"))
				.andExpect(jsonPath("$.data.recruitEndAt").value("2026-12-08T18:00:00"))
				.andExpect(jsonPath("$.data.createdAt").value("2026-11-01T09:00:00"))
				.andExpect(jsonPath("$.data.distanceKm").isEmpty())
				.andExpect(jsonPath("$.data.bookmarked").value(false))
				.andExpect(jsonPath("$.data.shelter.shelterId").value(1))
				.andExpect(jsonPath("$.data.shelter.name").value("성북 행복보호소"))
				.andExpect(jsonPath("$.data.shelter.region.sido").value("서울특별시"))
				.andExpect(jsonPath("$.data.shelter.address").value("서울특별시 성북구 1번지"))
				.andExpect(jsonPath("$.data.shelter.phone").value("02-000-0001"))
				.andExpect(jsonPath("$.data.shelter.verificationStatus").value("VERIFIED"))
				.andExpect(jsonPath("$.data.volunteer.activityCategories[0]").value("WALK"))
				.andExpect(jsonPath("$.data.volunteer.activityCategories[1]").value("FEEDING_CARE"))
				.andExpect(jsonPath("$.data.volunteer.activityStartAt").value("2026-12-10T10:00:00"))
				.andExpect(jsonPath("$.data.volunteer.activityEndAt").value("2026-12-10T12:00:00"))
				.andExpect(jsonPath("$.data.volunteer.capacity").value(10))
				.andExpect(jsonPath("$.data.volunteer.applicantCount").value(3))
				.andExpect(jsonPath("$.data.volunteer.remainingCount").value(7))
				.andExpect(jsonPath("$.data.volunteer.conditions.minAge").value(18))
				.andExpect(jsonPath("$.data.volunteer.conditions.beginnerFriendly").value(false))
				.andExpect(jsonPath("$.data.volunteer.conditions.requirements").value("산책 경험자"))
				.andExpect(jsonPath("$.data.sponsor").isEmpty())
				.andExpect(jsonPath("$.data.foster").isEmpty());
	}

	@Test
	void 후원_공고는_후원_블록만_채우고_링크_자체는_주지_않는다() throws Exception {
		get("/api/v1/postings/202").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.volunteer").isEmpty())
				.andExpect(jsonPath("$.data.foster").isEmpty())
				.andExpect(jsonPath("$.data.sponsor.donationPurpose").value("담요 구입"))
				.andExpect(jsonPath("$.data.sponsor.donationLinkAvailable").value(true))
				.andExpect(jsonPath("$.data.sponsor.donationUrl").doesNotExist())
				.andExpect(jsonPath("$.data.sponsor.targetQuantity").value(30))
				.andExpect(jsonPath("$.data.sponsor.receivedQuantity").value(9))
				.andExpect(jsonPath("$.data.sponsor.progressRate").value(30));
		get("/api/v1/postings/203").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sponsor.donationLinkAvailable").value(false))
				.andExpect(jsonPath("$.data.sponsor.progressRate").isEmpty());
	}

	@Test
	void 임시보호_공고는_문의_방식을_보여_준다() throws Exception {
		get("/api/v1/postings/204").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.foster.contactMethod").value("APP"))
				.andExpect(jsonPath("$.data.foster.externalUrl").isEmpty())
				.andExpect(jsonPath("$.data.foster.petName").value("나비"))
				.andExpect(jsonPath("$.data.foster.petAgeText").value("약 6개월"))
				.andExpect(jsonPath("$.data.foster.petGender").value("FEMALE"))
				.andExpect(jsonPath("$.data.foster.petCharacteristics").value("사람을 잘 따름"))
				.andExpect(jsonPath("$.data.volunteer").isEmpty())
				.andExpect(jsonPath("$.data.sponsor").isEmpty());
		get("/api/v1/postings/205").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.foster.contactMethod").value("EXTERNAL"))
				.andExpect(jsonPath("$.data.foster.externalUrl").value("https://example.org/foster"))
				.andExpect(jsonPath("$.data.foster.petName").isEmpty());
	}

	@Test
	void 미인증_시설의_연락처는_가린다() throws Exception {
		get("/api/v1/postings/202").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.shelter.verificationStatus").value("UNVERIFIED"))
				.andExpect(jsonPath("$.data.shelter.phone").isEmpty());
	}

	@Test
	void 종료된_공고도_상세는_볼_수_있다() throws Exception {
		get("/api/v1/postings/207").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("CLOSED"))
				.andExpect(jsonPath("$.data.volunteer.remainingCount").value(0));
	}

	@Test
	void 좌표를_보내면_거리를_주고_내_관심_여부를_반영한다() throws Exception {
		bookmarkRepository.save(PostingBookmarkRef.of(user.getUserId(), 202L, LocalDateTime.of(2026, 11, 1, 9, 0)));
		bookmarkRepository.save(PostingBookmarkRef.of(other.getUserId(), 201L, LocalDateTime.of(2026, 11, 1, 9, 0)));

		get("/api/v1/postings/202?latitude=37.5894&longitude=127.0167").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.distanceKm").value(org.hamcrest.Matchers.closeTo(8.0, 1.0)))
				.andExpect(jsonPath("$.data.bookmarked").value(true));
		get("/api/v1/postings/201").andExpect(jsonPath("$.data.bookmarked").value(false));
	}

	@Test
	void 비공개이거나_없는_공고는_404() throws Exception {
		get("/api/v1/postings/206").andExpect(status().isNotFound())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("POSTING_404_1"));
		get("/api/v1/postings/9999").andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("POSTING_404_1"));
	}

	@Test
	void 공고_ID와_좌표가_틀리면_400() throws Exception {
		get("/api/v1/postings/0").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		get("/api/v1/postings/201?latitude=37.5").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		get("/api/v1/postings/201?latitude=91&longitude=127").andExpect(status().isBadRequest());
		get("/api/v1/postings/abc").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	// ---- 봉사 가능 일정 ----

	@Test
	void 아직_시작하지_않은_일정만_시작_순으로_보여_주고_신청_가능_여부를_계산한다() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		scheduleRepository.save(PostingSchedule.of(201L, now.plusDays(9), now.plusDays(9).plusHours(2), 6, 6));
		scheduleRepository.save(PostingSchedule.of(201L, now.plusDays(2), now.plusDays(2).plusHours(2), 5, 1));
		scheduleRepository.save(PostingSchedule.of(201L, now.minusDays(1), now.minusDays(1).plusHours(2), 5, 0));
		scheduleRepository.save(PostingSchedule.of(204L, now.plusDays(2), now.plusDays(2).plusHours(2), 5, 0));

		get("/api/v1/postings/201/schedules").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.postingId").value(201))
				.andExpect(jsonPath("$.data.items.length()").value(2))
				.andExpect(jsonPath("$.data.items[0].scheduleId").isNumber())
				.andExpect(jsonPath("$.data.items[0].capacity").value(5))
				.andExpect(jsonPath("$.data.items[0].appliedCount").value(1))
				.andExpect(jsonPath("$.data.items[0].remainingCount").value(4))
				.andExpect(jsonPath("$.data.items[0].applicable").value(true))
				.andExpect(jsonPath("$.data.items[1].remainingCount").value(0))
				.andExpect(jsonPath("$.data.items[1].applicable").value(false));
	}

	@Test
	void 모집이_끝난_공고의_일정은_신청할_수_없다() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		scheduleRepository.save(PostingSchedule.of(207L, now.plusDays(2), now.plusDays(2).plusHours(2), 5, 0));

		get("/api/v1/postings/207/schedules").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].remainingCount").value(5))
				.andExpect(jsonPath("$.data.items[0].applicable").value(false));
	}

	@Test
	void 일정이_없으면_빈_목록이다() throws Exception {
		get("/api/v1/postings/201/schedules").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isEmpty());
	}

	@Test
	void 봉사_공고가_아니면_일정과_질문을_조회할_수_없다() throws Exception {
		get("/api/v1/postings/202/schedules").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("POSTING_400_1"));
		get("/api/v1/postings/204/application-questions").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("POSTING_400_1"));
	}

	@Test
	void 일정과_질문도_없거나_비공개인_공고는_404이고_ID가_틀리면_400() throws Exception {
		get("/api/v1/postings/206/schedules").andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("POSTING_404_1"));
		get("/api/v1/postings/9999/application-questions").andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("POSTING_404_1"));
		get("/api/v1/postings/0/schedules").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		get("/api/v1/postings/abc/application-questions").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	// ---- 봉사 신청 질문 ----

	@Test
	void 신청_질문을_표시_순서대로_보여_준다() throws Exception {
		questionRepository.save(PostingApplicationQuestion.of(201L, QuestionType.TEXT, "전하고 싶은 말", false, null, 3));
		questionRepository.save(PostingApplicationQuestion.of(201L, QuestionType.SINGLE_CHOICE, "봉사 경험이 있나요?",
				true, List.of("처음이에요", "1~5회", "6회 이상"), 1));
		questionRepository.save(PostingApplicationQuestion.of(201L, QuestionType.MULTI_CHOICE, "가능한 활동",
				false, List.of("산책", "청소"), 2));
		questionRepository.save(PostingApplicationQuestion.of(204L, QuestionType.TEXT, "다른 공고 질문", true, null, 1));

		get("/api/v1/postings/201/application-questions").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.postingId").value(201))
				.andExpect(jsonPath("$.data.items.length()").value(3))
				.andExpect(jsonPath("$.data.items[0].questionId").isNumber())
				.andExpect(jsonPath("$.data.items[0].questionType").value("SINGLE_CHOICE"))
				.andExpect(jsonPath("$.data.items[0].label").value("봉사 경험이 있나요?"))
				.andExpect(jsonPath("$.data.items[0].required").value(true))
				.andExpect(jsonPath("$.data.items[0].options[0]").value("처음이에요"))
				.andExpect(jsonPath("$.data.items[0].options.length()").value(3))
				.andExpect(jsonPath("$.data.items[0].sortOrder").value(1))
				.andExpect(jsonPath("$.data.items[1].questionType").value("MULTI_CHOICE"))
				.andExpect(jsonPath("$.data.items[1].required").value(false))
				.andExpect(jsonPath("$.data.items[2].questionType").value("TEXT"))
				.andExpect(jsonPath("$.data.items[2].options").isEmpty());
	}

	@Test
	void 질문이_없으면_빈_목록이다() throws Exception {
		get("/api/v1/postings/201/application-questions").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isEmpty());
	}

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/postings/201"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("COMMON_401_1"));
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/postings/201/schedules"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.get("/api/v1/postings/201/application-questions"))
				.andExpect(status().isUnauthorized());
	}
}

package com.jjonggeut.domain.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jjonggeut.domain.activity.entity.ApplicationAnswer;
import com.jjonggeut.domain.activity.entity.ApplicationStatus;
import com.jjonggeut.domain.activity.entity.VolunteerApplicationRef;
import com.jjonggeut.domain.application.entity.ApplicationExperience;
import com.jjonggeut.domain.application.entity.ApplicationStatusHistory;
import com.jjonggeut.domain.application.entity.StatusActor;
import com.jjonggeut.domain.application.repository.ApplicationExperienceRepository;
import com.jjonggeut.domain.posting.entity.PostingApplicationQuestion;
import com.jjonggeut.domain.posting.entity.QuestionType;
import com.jjonggeut.domain.posting.repository.PostingApplicationQuestionRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApplicationQueryTest extends ApplicationTestBase {

	@Autowired
	PostingApplicationQuestionRepository questionRepository;

	@Autowired
	VolunteerExperienceRepository experienceRepository;

	@Autowired
	ApplicationExperienceRepository applicationExperienceRepository;

	Long scheduleId;

	@BeforeEach
	void setUpData() {
		scheduleId = schedule(501, 72, 5, 1);
	}

	private ResultActions list(String query) throws Exception {
		return call(get("/api/v1/users/me/applications" + query), null);
	}

	private ResultActions detail(Object applicationId) throws Exception {
		return call(get("/api/v1/applications/" + applicationId), null);
	}

	private ResultActions history(Object applicationId) throws Exception {
		return call(get("/api/v1/applications/" + applicationId + "/status-history"), null);
	}

	// ---- 목록 ----

	@Test
	void 내_신청만_최근_신청순으로_보여_준다() throws Exception {
		Long first = application(user, 501, scheduleId, ApplicationStatus.PENDING);
		Long second = application(user, 502, null, ApplicationStatus.COMPLETED);
		application(other, 501, scheduleId, ApplicationStatus.PENDING);

		list("").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.data.items[0].applicationId").value(second))
				.andExpect(jsonPath("$.data.items[1].applicationId").value(first))
				.andExpect(jsonPath("$.data.items[1].status").value("PENDING"))
				.andExpect(jsonPath("$.data.items[1].postingId").value(501))
				.andExpect(jsonPath("$.data.items[1].title").value("주말 산책 봉사"))
				.andExpect(jsonPath("$.data.items[1].shelter.shelterId").value(1))
				.andExpect(jsonPath("$.data.items[1].shelter.name").value("성북 행복보호소"))
				.andExpect(jsonPath("$.data.items[1].scheduleId").value(scheduleId))
				.andExpect(jsonPath("$.data.items[1].activityStartAt").isNotEmpty())
				.andExpect(jsonPath("$.data.items[1].activityEndAt").isNotEmpty())
				.andExpect(jsonPath("$.data.items[1].appliedAt").isNotEmpty())
				.andExpect(jsonPath("$.data.items[1].cancellable").value(true))
				.andExpect(jsonPath("$.data.items[0].cancellable").value(false))
				.andExpect(jsonPath("$.data.items[0].scheduleId").isEmpty());
	}

	@Test
	void 상태_하나나_묶음으로_거른다() throws Exception {
		application(user, 501, scheduleId, ApplicationStatus.PENDING);
		application(user, 501, scheduleId, ApplicationStatus.CONFIRMED);
		application(user, 501, scheduleId, ApplicationStatus.COMPLETED);
		application(user, 501, scheduleId, ApplicationStatus.CANCELLED);
		application(user, 501, scheduleId, ApplicationStatus.REJECTED);
		application(user, 501, scheduleId, ApplicationStatus.NO_SHOW);

		list("?status=PENDING").andExpect(jsonPath("$.data.totalElements").value(1));
		list("?status=NO_SHOW").andExpect(jsonPath("$.data.totalElements").value(1));
		list("?status=ONGOING").andExpect(jsonPath("$.data.totalElements").value(2));
		list("?status=HISTORY").andExpect(jsonPath("$.data.totalElements").value(4));
		list("").andExpect(jsonPath("$.data.totalElements").value(6));
	}

	@Test
	void 일정이_없는_예전_신청은_공고의_시간을_보여_준다() throws Exception {
		application(user, 504, null, ApplicationStatus.CONFIRMED);

		list("").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].scheduleId").isEmpty())
				.andExpect(jsonPath("$.data.items[0].activityStartAt").isNotEmpty())
				.andExpect(jsonPath("$.data.items[0].activityEndAt").isNotEmpty())
				.andExpect(jsonPath("$.data.items[0].cancellable").value(true));
	}

	@Test
	void 취소_기한이_지난_신청은_취소할_수_없다고_알려_준다() throws Exception {
		application(user, 501, schedule(501, 5, 5, 1), ApplicationStatus.CONFIRMED);

		list("").andExpect(jsonPath("$.data.items[0].cancellable").value(false));
	}

	@Test
	void 페이지를_나눈다() throws Exception {
		for (int i = 0; i < 3; i++) {
			application(user, 501, scheduleId, ApplicationStatus.PENDING);
		}

		list("?page=0&size=2").andExpect(jsonPath("$.data.items.length()").value(2))
				.andExpect(jsonPath("$.data.totalElements").value(3))
				.andExpect(jsonPath("$.data.totalPages").value(2))
				.andExpect(jsonPath("$.data.hasNext").value(true));
		list("?page=1&size=2").andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.hasNext").value(false));
	}

	@Test
	void 신청이_없으면_빈_목록이다() throws Exception {
		list("").andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty())
				.andExpect(jsonPath("$.data.totalElements").value(0));
	}

	@Test
	void 상태와_페이지가_틀리면_400() throws Exception {
		list("?status=DONE").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		list("?status=pending").andExpect(status().isBadRequest());
		list("?page=-1").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		list("?size=0").andExpect(status().isBadRequest());
		list("?size=51").andExpect(status().isBadRequest());
		list("?page=abc").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 목록은_토큰이_없으면_401() throws Exception {
		mockMvc.perform(get("/api/v1/users/me/applications")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}

	// ---- 상세 ----

	@Test
	void 신청_상세를_보여_준다() throws Exception {
		Long q1 = questionRepository.saveAndFlush(PostingApplicationQuestion.of(501L, QuestionType.TEXT, "하고 싶은 말",
				false, null, 2)).getQuestionId();
		Long q2 = questionRepository.saveAndFlush(PostingApplicationQuestion.of(501L, QuestionType.SINGLE_CHOICE,
				"봉사 경험", true, List.of("처음이에요", "1~5회"), 1)).getQuestionId();
		questionRepository.saveAndFlush(PostingApplicationQuestion.of(501L, QuestionType.TEXT, "답하지 않은 질문", false,
				null, 3));
		LocalDateTime appliedAt = LocalDateTime.now().minusHours(2).withNano(0);
		VolunteerApplicationRef saved = applicationRepository.saveAndFlush(VolunteerApplicationRef.apply(
				user.getUserId(), 501L, scheduleId,
				List.of(new ApplicationAnswer(q1, List.of("열심히 할게요")), new ApplicationAnswer(q2, List.of("처음이에요"))),
				"잘 부탁드립니다", appliedAt));
		historyRepository.saveAndFlush(ApplicationStatusHistory.of(saved.getApplicationId(), null,
				ApplicationStatus.PENDING, StatusActor.USER, null, appliedAt));
		historyRepository.saveAndFlush(ApplicationStatusHistory.of(saved.getApplicationId(), ApplicationStatus.PENDING,
				ApplicationStatus.CONFIRMED, StatusActor.FACILITY, null, appliedAt.plusHours(1)));
		saved.changeStatus(ApplicationStatus.CONFIRMED);
		applicationRepository.saveAndFlush(saved);
		var schedule = scheduleRepository.findById(scheduleId).orElseThrow();

		detail(saved.getApplicationId()).andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.applicationId").value(saved.getApplicationId()))
				.andExpect(jsonPath("$.data.status").value("CONFIRMED"))
				.andExpect(jsonPath("$.data.appliedAt").value(appliedAt.format(ISO)))
				.andExpect(jsonPath("$.data.message").value("잘 부탁드립니다"))
				.andExpect(jsonPath("$.data.answers.length()").value(2))
				.andExpect(jsonPath("$.data.answers[0].questionId").value(q2))
				.andExpect(jsonPath("$.data.answers[0].label").value("봉사 경험"))
				.andExpect(jsonPath("$.data.answers[0].values[0]").value("처음이에요"))
				.andExpect(jsonPath("$.data.answers[1].label").value("하고 싶은 말"))
				.andExpect(jsonPath("$.data.posting.postingId").value(501))
				.andExpect(jsonPath("$.data.posting.title").value("주말 산책 봉사"))
				.andExpect(jsonPath("$.data.posting.postingType").value("VOLUNTEER"))
				.andExpect(jsonPath("$.data.posting.status").value("OPEN"))
				.andExpect(jsonPath("$.data.shelter.shelterId").value(1))
				.andExpect(jsonPath("$.data.shelter.name").value("성북 행복보호소"))
				.andExpect(jsonPath("$.data.shelter.address").value("서울특별시 성북구 1번지"))
				.andExpect(jsonPath("$.data.shelter.phone").value("02-000-0001"))
				.andExpect(jsonPath("$.data.schedule.scheduleId").value(scheduleId))
				.andExpect(jsonPath("$.data.schedule.startAt").value(schedule.getStartAt().format(ISO)))
				.andExpect(jsonPath("$.data.schedule.endAt").value(schedule.getEndAt().format(ISO)))
				.andExpect(jsonPath("$.data.preparations[0]").value("편한 복장"))
				.andExpect(jsonPath("$.data.preparations[1]").value("운동화"))
				.andExpect(jsonPath("$.data.cancellable").value(true))
				.andExpect(jsonPath("$.data.cancelDeadlineAt").value(schedule.getStartAt().minusHours(24).format(ISO)))
				.andExpect(jsonPath("$.data.statusHistory.length()").value(2))
				.andExpect(jsonPath("$.data.statusHistory[0].toStatus").value("PENDING"))
				.andExpect(jsonPath("$.data.statusHistory[1].actor").value("FACILITY"));
	}

	@Test
	void 미인증_시설의_연락처는_가리고_준비물이_없으면_빈_목록이다() throws Exception {
		Long id = application(user, 504, null, ApplicationStatus.PENDING);

		detail(id).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.shelter.shelterId").value(2))
				.andExpect(jsonPath("$.data.shelter.phone").isEmpty())
				.andExpect(jsonPath("$.data.preparations").isEmpty())
				.andExpect(jsonPath("$.data.answers").isEmpty())
				.andExpect(jsonPath("$.data.message").isEmpty());
	}

	@Test
	void 일정이_없는_예전_신청은_공고의_활동_시간을_일정으로_보여_준다() throws Exception {
		Long id = application(user, 504, null, ApplicationStatus.CONFIRMED);

		detail(id).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.schedule.scheduleId").isEmpty())
				.andExpect(jsonPath("$.data.schedule.startAt").isNotEmpty())
				.andExpect(jsonPath("$.data.cancelDeadlineAt").isNotEmpty());
	}

	@Test
	void 일정_정보가_전혀_없으면_일정과_취소_기한은_null이다() throws Exception {
		Long id = application(user, 501, null, ApplicationStatus.PENDING);

		detail(id).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.schedule").isEmpty())
				.andExpect(jsonPath("$.data.cancelDeadlineAt").isEmpty())
				.andExpect(jsonPath("$.data.cancellable").value(true));
	}

	@Test
	void 경험을_첨부하지_않은_신청은_상세에서_experience가_null이다() throws Exception {
		Long id = application(user, 501, null, ApplicationStatus.PENDING);

		detail(id).andExpect(status().isOk()).andExpect(jsonPath("$.data.experience").isEmpty());
	}

	@Test
	void 첨부한_경험은_상세에서_스냅샷으로_보인다() throws Exception {
		VolunteerExperience experience = experienceRepository.saveAndFlush(VolunteerExperience.create(
				user.getUserId(), List.of(ActivityCategory.WALK), "성북 행복보호소", "2026.01",
				"강아지 산책 봉사를 했습니다.", null));
		Long id = application(user, 501, null, ApplicationStatus.PENDING);
		applicationExperienceRepository.saveAndFlush(
				ApplicationExperience.of(id, experience.getExperienceId(), "강아지 산책 봉사를 했습니다."));

		detail(id).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.experience.experienceId").value(experience.getExperienceId()))
				.andExpect(jsonPath("$.data.experience.snapshot").value("강아지 산책 봉사를 했습니다."));
	}

	@Test
	void 비공개로_바뀐_공고의_내_신청도_볼_수_있다() throws Exception {
		Long id = application(user, 503, schedule(503, 72, 5, 1), ApplicationStatus.PENDING);

		detail(id).andExpect(status().isOk()).andExpect(jsonPath("$.data.posting.postingId").value(503));
	}

	@Test
	void 끝난_신청은_취소할_수_없다고_알려_준다() throws Exception {
		Long id = application(user, 501, scheduleId, ApplicationStatus.COMPLETED);

		detail(id).andExpect(jsonPath("$.data.cancellable").value(false));
	}

	@Test
	void 남의_신청이거나_없는_신청은_상세도_이력도_404() throws Exception {
		Long othersId = application(other, 501, scheduleId, ApplicationStatus.PENDING);

		detail(othersId).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("APPLICATION_404_1"));
		history(othersId).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("APPLICATION_404_1"));
		detail(999999).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("APPLICATION_404_1"));
		history(999999).andExpect(status().isNotFound());
	}

	@Test
	void 신청_ID가_틀리면_400() throws Exception {
		detail(0).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		history(-1).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		detail("abc").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
		history("abc").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 상세와_이력은_토큰이_없으면_401() throws Exception {
		mockMvc.perform(get("/api/v1/applications/1")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/applications/1/status-history")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}

	// ---- 상태 이력 ----

	@Test
	void 상태_이력을_오래된_순으로_보여_준다() throws Exception {
		Long id = application(user, 501, scheduleId, ApplicationStatus.CANCELLED);
		LocalDateTime base = LocalDateTime.now().minusHours(5).withNano(0);
		historyRepository.saveAndFlush(ApplicationStatusHistory.of(id, ApplicationStatus.PENDING,
				ApplicationStatus.CANCELLED, StatusActor.USER, "일정이 바뀌었어요", base.plusHours(2)));
		historyRepository.saveAndFlush(ApplicationStatusHistory.of(id, ApplicationStatus.PENDING,
				ApplicationStatus.CONFIRMED, StatusActor.FACILITY, null, base.plusHours(1)));

		history(id).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.applicationId").value(id))
				.andExpect(jsonPath("$.data.currentStatus").value("CANCELLED"))
				.andExpect(jsonPath("$.data.items.length()").value(3))
				.andExpect(jsonPath("$.data.items[0].fromStatus").isEmpty())
				.andExpect(jsonPath("$.data.items[0].toStatus").value("PENDING"))
				.andExpect(jsonPath("$.data.items[0].actor").value("USER"))
				.andExpect(jsonPath("$.data.items[1].toStatus").value("CONFIRMED"))
				.andExpect(jsonPath("$.data.items[1].actor").value("FACILITY"))
				.andExpect(jsonPath("$.data.items[1].historyId").isNumber())
				.andExpect(jsonPath("$.data.items[2].toStatus").value("CANCELLED"))
				.andExpect(jsonPath("$.data.items[2].reason").value("일정이 바뀌었어요"))
				.andExpect(jsonPath("$.data.items[2].changedAt").value(base.plusHours(2).format(ISO)));
	}

	@Test
	void 이력이_없는_신청은_빈_목록이다() throws Exception {
		VolunteerApplicationRef saved = applicationRepository.saveAndFlush(VolunteerApplicationRef.of(user.getUserId(),
				501L, scheduleId, ApplicationStatus.PENDING, LocalDateTime.now().withNano(0)));

		history(saved.getApplicationId()).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isEmpty())
				.andExpect(jsonPath("$.data.currentStatus").value("PENDING"));
	}
}

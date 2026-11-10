package com.jjonggeut.domain.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jjonggeut.domain.activity.entity.ApplicationStatus;
import com.jjonggeut.domain.posting.entity.PostingApplicationQuestion;
import com.jjonggeut.domain.posting.entity.QuestionType;
import com.jjonggeut.domain.posting.repository.PostingApplicationQuestionRepository;
import com.jjonggeut.domain.recommendation.entity.ActivityEventType;
import com.jjonggeut.domain.recommendation.entity.PostingRef;
import com.jjonggeut.domain.recommendation.repository.UserActivityEventRepository;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
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
class ApplicationApplyTest extends ApplicationTestBase {

	@Autowired
	PostingApplicationQuestionRepository questionRepository;

	@Autowired
	UserActivityEventRepository eventRepository;

	Long open;

	@BeforeEach
	void setUpData() {
		open = schedule(501, 72, 3, 0);
	}

	private ResultActions apply(Object scheduleId, String body) throws Exception {
		return call(post("/api/v1/schedules/" + scheduleId + "/applications"), body);
	}

	private Long question(QuestionType type, String label, boolean required, List<String> options, int order) {
		return questionRepository.saveAndFlush(PostingApplicationQuestion.of(501L, type, label, required, options, order))
				.getQuestionId();
	}

	@Test
	void 승인_대기로_접수하고_정원_이력_이벤트를_남긴다() throws Exception {
		apply(open, null).andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.applicationId").isNumber())
				.andExpect(jsonPath("$.data.scheduleId").value(open))
				.andExpect(jsonPath("$.data.postingId").value(501))
				.andExpect(jsonPath("$.data.status").value("PENDING"))
				.andExpect(jsonPath("$.data.activityStartAt").isNotEmpty())
				.andExpect(jsonPath("$.data.activityEndAt").isNotEmpty())
				.andExpect(jsonPath("$.data.appliedAt").isNotEmpty());

		assertThat(appliedCount(open)).isEqualTo(1);
		assertThat(applicantCount(501)).isEqualTo(1);
		var saved = applicationRepository.findAll();
		assertThat(saved).hasSize(1);
		assertThat(saved.get(0).getUserId()).isEqualTo(user.getUserId());
		assertThat(saved.get(0).getStatus()).isEqualTo(ApplicationStatus.PENDING);
		var history = historyRepository.findAll();
		assertThat(history).hasSize(1);
		assertThat(history.get(0).getFromStatus()).isNull();
		assertThat(history.get(0).getToStatus()).isEqualTo(ApplicationStatus.PENDING);
		var events = eventRepository.findAll();
		assertThat(events).hasSize(1);
		assertThat(events.get(0).getEventType()).isEqualTo(ActivityEventType.APPLY);
		assertThat(events.get(0).getPostingId()).isEqualTo(501L);
		assertThat(events.get(0).getShelterId()).isEqualTo(1L);
	}

	@Test
	void 전할_말은_앞뒤_공백을_제거하고_비면_저장하지_않는다() throws Exception {
		apply(open, "{\"message\":\"  잘 부탁드립니다  \"}").andExpect(status().isOk());
		assertThat(applicationRepository.findAll().get(0).getMessage()).isEqualTo("잘 부탁드립니다");

		Long second = schedule(501, 96, 3, 0);
		apply(second, "{\"message\":\"   \"}").andExpect(status().isOk());
		assertThat(applicationRepository.findAll().get(1).getMessage()).isNull();
	}

	@Test
	void 답변을_받아_저장한다() throws Exception {
		Long q1 = question(QuestionType.SINGLE_CHOICE, "봉사 경험이 있나요?", true, List.of("처음이에요", "1~5회"), 1);
		Long q2 = question(QuestionType.MULTI_CHOICE, "가능한 활동", false, List.of("산책", "청소", "목욕"), 2);
		Long q3 = question(QuestionType.TEXT, "하고 싶은 말", false, null, 3);

		apply(open, "{\"answers\":[{\"questionId\":" + q1 + ",\"values\":[\"처음이에요\"]},"
				+ "{\"questionId\":" + q2 + ",\"values\":[\"산책\",\"청소\"]},"
				+ "{\"questionId\":" + q3 + ",\"values\":[\"  열심히 할게요  \"]}]}").andExpect(status().isOk());

		refresh();
		var answers = applicationRepository.findAll().get(0).getAnswers();
		assertThat(answers).hasSize(3);
		assertThat(answers.get(0).questionId()).isEqualTo(q1);
		assertThat(answers.get(1).values()).containsExactly("산책", "청소");
		assertThat(answers.get(2).values()).containsExactly("열심히 할게요");
	}

	@Test
	void 필수_질문에_답하지_않으면_400() throws Exception {
		question(QuestionType.SINGLE_CHOICE, "봉사 경험이 있나요?", true, List.of("처음이에요"), 1);

		apply(open, null).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("APPLICATION_400_1"));
		apply(open, "{\"answers\":[]}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("APPLICATION_400_1"));
		assertThat(applicationRepository.count()).isZero();
		assertThat(appliedCount(open)).isZero();
	}

	@Test
	void 질문에_맞지_않는_답변은_400() throws Exception {
		Long single = question(QuestionType.SINGLE_CHOICE, "경험", false, List.of("처음이에요", "1~5회"), 1);
		Long multi = question(QuestionType.MULTI_CHOICE, "활동", false, List.of("산책", "청소"), 2);
		Long text = question(QuestionType.TEXT, "한마디", false, null, 3);

		// 없는 질문
		apply(open, "{\"answers\":[{\"questionId\":999999,\"values\":[\"a\"]}]}")
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("APPLICATION_400_1"));
		// 같은 질문 두 번
		apply(open, "{\"answers\":[{\"questionId\":" + text + ",\"values\":[\"a\"]},"
				+ "{\"questionId\":" + text + ",\"values\":[\"b\"]}]}")
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("APPLICATION_400_1"));
		// 선택지 밖의 값
		apply(open, "{\"answers\":[{\"questionId\":" + single + ",\"values\":[\"없는 선택지\"]}]}")
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("APPLICATION_400_1"));
		// 단일 선택에 두 개
		apply(open, "{\"answers\":[{\"questionId\":" + single + ",\"values\":[\"처음이에요\",\"1~5회\"]}]}")
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("APPLICATION_400_1"));
		// 다중 선택에 중복
		apply(open, "{\"answers\":[{\"questionId\":" + multi + ",\"values\":[\"산책\",\"산책\"]}]}")
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("APPLICATION_400_1"));
		// 주관식에 두 개
		apply(open, "{\"answers\":[{\"questionId\":" + text + ",\"values\":[\"a\",\"b\"]}]}")
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("APPLICATION_400_1"));
		assertThat(applicationRepository.count()).isZero();
	}

	@Test
	void 답변_형식이_틀리면_COMMON_400() throws Exception {
		apply(open, "{\"answers\":[{\"questionId\":1,\"values\":[]}]}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		apply(open, "{\"answers\":[{\"questionId\":1,\"values\":[\"  \"]}]}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		apply(open, "{\"answers\":[{\"values\":[\"a\"]}]}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		apply(open, "{\"answers\":[{\"questionId\":0,\"values\":[\"a\"]}]}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		apply(open, "{\"message\":\"" + "가".repeat(501) + "\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		apply(open, "{broken").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 같은_일정에_진행_중인_신청이_있으면_409() throws Exception {
		apply(open, null).andExpect(status().isOk());

		apply(open, null).andExpect(status().isConflict())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("APPLICATION_409_1"));
		assertThat(applicationRepository.count()).isEqualTo(1);
		assertThat(appliedCount(open)).isEqualTo(1);
	}

	@Test
	void 확정된_신청이_있어도_409이고_끝난_신청은_다시_신청할_수_있다() throws Exception {
		Long confirmedSchedule = schedule(501, 100, 3, 1);
		application(user, 501, confirmedSchedule, ApplicationStatus.CONFIRMED);
		apply(confirmedSchedule, null).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("APPLICATION_409_1"));

		Long doneSchedule = schedule(501, 120, 3, 0);
		application(user, 501, doneSchedule, ApplicationStatus.CANCELLED);
		application(user, 501, doneSchedule, ApplicationStatus.REJECTED);
		apply(doneSchedule, null).andExpect(status().isOk());
	}

	@Test
	void 다른_사용자가_신청한_일정도_신청할_수_있다() throws Exception {
		application(other, 501, open, ApplicationStatus.PENDING);

		apply(open, null).andExpect(status().isOk());
	}

	@Test
	void 정원이_가득_차면_409이고_숫자는_그대로다() throws Exception {
		Long full = schedule(501, 72, 2, 2);

		apply(full, null).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("APPLICATION_409_2"));
		assertThat(appliedCount(full)).isEqualTo(2);
		assertThat(applicantCount(501)).isZero();
		assertThat(applicationRepository.count()).isZero();
	}

	@Test
	void 마지막_자리는_한_명만_신청할_수_있다() throws Exception {
		Long last = schedule(501, 72, 1, 0);

		apply(last, null).andExpect(status().isOk());
		call(post("/api/v1/schedules/" + last + "/applications"), otherToken, null).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("APPLICATION_409_2"));
		assertThat(appliedCount(last)).isEqualTo(1);
	}

	@Test
	void 모집이_끝난_공고나_이미_시작한_일정은_409() throws Exception {
		apply(schedule(502, 72, 3, 0), null).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("APPLICATION_409_3"));
		apply(schedule(501, -1, 3, 0), null).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("APPLICATION_409_3"));
	}

	@Test
	void 모집_마감_시각이_지난_공고는_409() throws Exception {
		postingRepository.saveAndFlush(PostingRef.of(505L, 1L, "VOLUNTEER", "마감 지난 봉사", "OPEN",
				List.of(ActivityCategory.WALK), "DOG", null, 5, 0)
				.withPeriod(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(5)));
		Long late = schedule(505, 72, 3, 0);

		apply(late, null).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("APPLICATION_409_3"));
	}

	@Test
	void 없거나_비공개인_공고의_일정은_404() throws Exception {
		apply(999999, null).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("APPLICATION_404_2"));
		apply(schedule(503, 72, 3, 0), null).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("APPLICATION_404_2"));
	}

	@Test
	void 일정_ID가_1_미만이면_400이고_숫자가_아니면_COMMON_400_1() throws Exception {
		apply(0, null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		apply("abc", null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(post("/api/v1/schedules/" + open + "/applications")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}
}

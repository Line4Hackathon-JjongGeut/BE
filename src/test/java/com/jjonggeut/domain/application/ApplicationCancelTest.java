package com.jjonggeut.domain.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jjonggeut.domain.activity.entity.ApplicationAnswer;
import com.jjonggeut.domain.activity.entity.ApplicationStatus;
import com.jjonggeut.domain.activity.entity.VolunteerApplicationRef;
import com.jjonggeut.domain.application.entity.StatusActor;
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
class ApplicationCancelTest extends ApplicationTestBase {

	Long far;
	Long pendingId;

	@BeforeEach
	void setUpData() {
		far = schedule(501, 72, 5, 1);
		pendingId = application(user, 501, far, ApplicationStatus.PENDING);
	}

	private ResultActions cancel(Object applicationId, String body) throws Exception {
		return call(delete("/api/v1/applications/" + applicationId), body);
	}

	@Test
	void 승인_대기_신청을_취소하면_자리를_돌려주고_이력을_남긴다() throws Exception {
		cancel(pendingId, null).andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.applicationId").value(pendingId))
				.andExpect(jsonPath("$.data.status").value("CANCELLED"))
				.andExpect(jsonPath("$.data.cancelledAt").isNotEmpty());

		assertThat(appliedCount(far)).isZero();
		assertThat(applicantCount(501)).isZero();
		assertThat(applicationRepository.findById(pendingId).orElseThrow().getStatus())
				.isEqualTo(ApplicationStatus.CANCELLED);
		var history = historyRepository.findByApplicationIdOrderByChangedAtAscHistoryIdAsc(pendingId);
		assertThat(history).hasSize(2);
		assertThat(history.get(1).getFromStatus()).isEqualTo(ApplicationStatus.PENDING);
		assertThat(history.get(1).getToStatus()).isEqualTo(ApplicationStatus.CANCELLED);
		assertThat(history.get(1).getActor()).isEqualTo(StatusActor.USER);
		assertThat(history.get(1).getReason()).isNull();
	}

	@Test
	void 확정된_신청도_기한_안이면_취소할_수_있다() throws Exception {
		Long id = application(user, 501, schedule(501, 48, 5, 1), ApplicationStatus.CONFIRMED);

		cancel(id, "{\"reason\":\"  일정이 생겼어요  \"}").andExpect(status().isOk());

		var history = historyRepository.findByApplicationIdOrderByChangedAtAscHistoryIdAsc(id);
		assertThat(history.get(1).getFromStatus()).isEqualTo(ApplicationStatus.CONFIRMED);
		assertThat(history.get(1).getReason()).isEqualTo("일정이 생겼어요");
	}

	@Test
	void 사유가_비어_있으면_저장하지_않는다() throws Exception {
		cancel(pendingId, "{\"reason\":\"   \"}").andExpect(status().isOk());

		assertThat(historyRepository.findByApplicationIdOrderByChangedAtAscHistoryIdAsc(pendingId).get(1).getReason())
				.isNull();
	}

	@Test
	void 일정_시작_24시간_안에는_취소할_수_없다() throws Exception {
		Long id = application(user, 501, schedule(501, 23, 5, 1), ApplicationStatus.CONFIRMED);

		cancel(id, null).andExpect(status().isConflict())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("APPLICATION_409_5"));
		assertThat(applicationRepository.findById(id).orElseThrow().getStatus()).isEqualTo(ApplicationStatus.CONFIRMED);
	}

	@Test
	void 이미_시작한_일정도_취소할_수_없다() throws Exception {
		Long id = application(user, 501, schedule(501, -1, 5, 1), ApplicationStatus.PENDING);

		cancel(id, null).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("APPLICATION_409_5"));
	}

	@Test
	void 진행_중이_아닌_신청은_취소할_수_없다() throws Exception {
		for (ApplicationStatus finished : List.of(ApplicationStatus.CANCELLED, ApplicationStatus.COMPLETED,
				ApplicationStatus.REJECTED, ApplicationStatus.NO_SHOW)) {
			Long id = application(user, 501, far, finished);
			cancel(id, null).andExpect(status().isConflict())
					.andExpect(jsonPath("$.code").value("APPLICATION_409_4"));
		}
	}

	@Test
	void 두_번_취소하면_두_번째는_409이고_자리는_한_번만_돌려준다() throws Exception {
		cancel(pendingId, null).andExpect(status().isOk());

		cancel(pendingId, null).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("APPLICATION_409_4"));
		assertThat(appliedCount(far)).isZero();
	}

	@Test
	void 일정이_없는_예전_신청은_공고의_활동_시작을_기준으로_취소한다() throws Exception {
		Long id = application(user, 504, null, ApplicationStatus.PENDING);

		cancel(id, null).andExpect(status().isOk());

		assertThat(applicantCount(504)).isEqualTo(1);
	}

	@Test
	void 남의_신청이거나_없는_신청은_404() throws Exception {
		Long othersId = application(other, 501, far, ApplicationStatus.PENDING);

		cancel(othersId, null).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("APPLICATION_404_1"));
		cancel(999999, null).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("APPLICATION_404_1"));
		assertThat(applicationRepository.findById(othersId).orElseThrow().getStatus())
				.isEqualTo(ApplicationStatus.PENDING);
	}

	@Test
	void 신청_ID_사유_길이가_틀리면_400() throws Exception {
		cancel(0, null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		cancel("abc", null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
		cancel(pendingId, "{\"reason\":\"" + "가".repeat(201) + "\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(delete("/api/v1/applications/" + pendingId)).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}

	@Test
	void 회원_탈퇴하면_진행_중_신청은_자동_취소되고_답변은_비워진다() throws Exception {
		VolunteerApplicationRef withData = applicationRepository.saveAndFlush(VolunteerApplicationRef.apply(
				user.getUserId(), 501L, far, List.of(new ApplicationAnswer(1L, List.of("답"))), "전할 말",
				LocalDateTime.now().minusHours(3).withNano(0)));
		Long doneId = application(user, 501, far, ApplicationStatus.COMPLETED);
		Long othersId = application(other, 501, far, ApplicationStatus.PENDING);

		call(delete("/api/v1/users/me"), null).andExpect(status().isOk());

		refresh();
		assertThat(applicationRepository.findById(pendingId).orElseThrow().getStatus())
				.isEqualTo(ApplicationStatus.CANCELLED);
		assertThat(applicationRepository.findById(withData.getApplicationId()).orElseThrow().getStatus())
				.isEqualTo(ApplicationStatus.CANCELLED);
		var cleared = applicationRepository.findById(withData.getApplicationId()).orElseThrow();
		assertThat(cleared.getAnswers()).isEmpty();
		assertThat(cleared.getMessage()).isNull();
		assertThat(applicationRepository.findById(doneId).orElseThrow().getStatus())
				.isEqualTo(ApplicationStatus.COMPLETED);
		assertThat(applicationRepository.findById(othersId).orElseThrow().getStatus())
				.isEqualTo(ApplicationStatus.PENDING);
		var history = historyRepository.findByApplicationIdOrderByChangedAtAscHistoryIdAsc(pendingId);
		assertThat(history.get(history.size() - 1).getActor()).isEqualTo(StatusActor.SYSTEM);
		assertThat(history.get(history.size() - 1).getReason()).isEqualTo("회원 탈퇴");
	}
}

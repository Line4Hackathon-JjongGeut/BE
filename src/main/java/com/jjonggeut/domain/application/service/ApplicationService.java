package com.jjonggeut.domain.application.service;

import com.jjonggeut.domain.activity.entity.ApplicationAnswer;
import com.jjonggeut.domain.activity.entity.ApplicationStatus;
import com.jjonggeut.domain.activity.entity.VolunteerApplicationRef;
import com.jjonggeut.domain.activity.repository.VolunteerApplicationRefRepository;
import com.jjonggeut.domain.application.dto.ApplicationCancelRequest;
import com.jjonggeut.domain.application.dto.ApplicationCancelResponse;
import com.jjonggeut.domain.application.dto.ApplicationRequest;
import com.jjonggeut.domain.application.dto.ApplicationResponse;
import com.jjonggeut.domain.application.entity.ApplicationStatusHistory;
import com.jjonggeut.domain.application.entity.StatusActor;
import com.jjonggeut.domain.application.exception.ApplicationErrorCode;
import com.jjonggeut.domain.application.repository.ApplicationStatusHistoryRepository;
import com.jjonggeut.domain.posting.entity.PostingApplicationQuestion;
import com.jjonggeut.domain.posting.entity.PostingSchedule;
import com.jjonggeut.domain.posting.repository.PostingApplicationQuestionRepository;
import com.jjonggeut.domain.posting.repository.PostingScheduleRepository;
import com.jjonggeut.domain.recommendation.dto.ActivityEventRequest;
import com.jjonggeut.domain.recommendation.entity.ActivityEventType;
import com.jjonggeut.domain.recommendation.entity.PostingRef;
import com.jjonggeut.domain.recommendation.repository.PostingRefRepository;
import com.jjonggeut.domain.recommendation.service.ActivityEventService;
import com.jjonggeut.global.exception.BusinessException;
import com.jjonggeut.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 봉사 신청과 신청 취소. 정원은 일정의 신청 인원을 DB 에서 조건부로 올려 지킨다. */
@Service
@Transactional
public class ApplicationService {

	private static final String WITHDRAW_REASON = "회원 탈퇴";

	private final VolunteerApplicationRefRepository applicationRepository;
	private final ApplicationStatusHistoryRepository historyRepository;
	private final PostingScheduleRepository scheduleRepository;
	private final PostingRefRepository postingRepository;
	private final PostingApplicationQuestionRepository questionRepository;
	private final ActivityEventService activityEventService;

	public ApplicationService(VolunteerApplicationRefRepository applicationRepository,
			ApplicationStatusHistoryRepository historyRepository, PostingScheduleRepository scheduleRepository,
			PostingRefRepository postingRepository, PostingApplicationQuestionRepository questionRepository,
			ActivityEventService activityEventService) {
		this.applicationRepository = applicationRepository;
		this.historyRepository = historyRepository;
		this.scheduleRepository = scheduleRepository;
		this.postingRepository = postingRepository;
		this.questionRepository = questionRepository;
		this.activityEventService = activityEventService;
	}

	/** 승인 대기 상태로 접수한다. request 가 없으면 답변도 전할 말도 없는 신청이다. */
	public ApplicationResponse apply(Long userId, Long scheduleId, ApplicationRequest request) {
		if (scheduleId == null || scheduleId < 1) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		PostingSchedule schedule = scheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new BusinessException(ApplicationErrorCode.SCHEDULE_NOT_FOUND));
		PostingRef posting = postingRepository.findById(schedule.getPostingId())
				.filter(PostingRef::isPublicPosting)
				.orElseThrow(() -> new BusinessException(ApplicationErrorCode.SCHEDULE_NOT_FOUND));

		LocalDateTime now = LocalDateTime.now();
		boolean recruitEnded = posting.getRecruitEndAt() != null && posting.getRecruitEndAt().isBefore(now);
		if (!PostingRef.STATUS_OPEN.equals(posting.getStatus()) || recruitEnded
				|| !schedule.getStartAt().isAfter(now)) {
			throw new BusinessException(ApplicationErrorCode.NOT_ACCEPTING);
		}
		if (applicationRepository.existsByUserIdAndScheduleIdAndStatusIn(userId, scheduleId,
				ApplicationStatus.ONGOING)) {
			throw new BusinessException(ApplicationErrorCode.ALREADY_APPLIED);
		}

		List<ApplicationAnswer> answers = validateAnswers(posting.getPostingId(),
				request == null ? null : request.answers());
		String message = request == null ? null : emptyToNull(request.message());

		if (scheduleRepository.increaseApplied(scheduleId) == 0) {
			throw new BusinessException(ApplicationErrorCode.SCHEDULE_FULL);
		}
		postingRepository.increaseApplicants(posting.getPostingId());

		VolunteerApplicationRef application = applicationRepository.saveAndFlush(VolunteerApplicationRef.apply(userId,
				posting.getPostingId(), scheduleId, answers, message, now));
		historyRepository.save(ApplicationStatusHistory.of(application.getApplicationId(), null,
				ApplicationStatus.PENDING, StatusActor.USER, null, now));
		activityEventService.record(userId, new ActivityEventRequest(ActivityEventType.APPLY,
				posting.getPostingId(), posting.getShelterId(), null, null));

		return new ApplicationResponse(application.getApplicationId(), scheduleId, posting.getPostingId(),
				application.getStatus(), schedule.getStartAt(), schedule.getEndAt(), application.getAppliedAt());
	}

	/** 진행 중인 내 신청을 일정 시작 24시간 전까지 취소한다. */
	public ApplicationCancelResponse cancel(Long userId, Long applicationId, ApplicationCancelRequest request) {
		VolunteerApplicationRef application = requireMine(userId, applicationId);
		if (!ApplicationStatus.ONGOING.contains(application.getStatus())) {
			throw new BusinessException(ApplicationErrorCode.NOT_CANCELLABLE_STATUS);
		}
		PostingSchedule schedule = application.getScheduleId() == null ? null
				: scheduleRepository.findById(application.getScheduleId()).orElse(null);
		PostingRef posting = postingRepository.findById(application.getPostingId()).orElse(null);
		LocalDateTime now = LocalDateTime.now();
		if (!ApplicationPolicy.beforeDeadline(ApplicationPolicy.startAt(schedule, posting), now)) {
			throw new BusinessException(ApplicationErrorCode.CANCEL_DEADLINE_PASSED);
		}

		String reason = request == null ? null : emptyToNull(request.reason());
		cancelApplication(application, StatusActor.USER, reason, now);
		return new ApplicationCancelResponse(applicationId, ApplicationStatus.CANCELLED, now);
	}

	/** 탈퇴하는 사용자의 진행 중 신청을 자동 취소해 자리를 돌려주고, 답변과 전할 말은 비운다. */
	public void releaseOnWithdraw(Long userId) {
		LocalDateTime now = LocalDateTime.now();
		for (VolunteerApplicationRef application : applicationRepository.findByUserId(userId)) {
			if (ApplicationStatus.ONGOING.contains(application.getStatus())) {
				cancelApplication(application, StatusActor.SYSTEM, WITHDRAW_REASON, now);
			}
			application.clearPersonalData();
		}
	}

	private void cancelApplication(VolunteerApplicationRef application, StatusActor actor, String reason,
			LocalDateTime now) {
		ApplicationStatus before = application.getStatus();
		application.changeStatus(ApplicationStatus.CANCELLED);
		applicationRepository.saveAndFlush(application);
		historyRepository.save(ApplicationStatusHistory.of(application.getApplicationId(), before,
				ApplicationStatus.CANCELLED, actor, reason, now));
		if (application.getScheduleId() != null) {
			scheduleRepository.decreaseApplied(application.getScheduleId());
		}
		postingRepository.decreaseApplicants(application.getPostingId());
	}

	private VolunteerApplicationRef requireMine(Long userId, Long applicationId) {
		if (applicationId == null || applicationId < 1) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		return applicationRepository.findByApplicationIdAndUserId(applicationId, userId)
				.orElseThrow(() -> new BusinessException(ApplicationErrorCode.APPLICATION_NOT_FOUND));
	}

	/** 질문에 없는 ID, 같은 질문 중복, 필수 누락, 선택지 밖의 값은 모두 APPLICATION_400_1 이다. */
	private List<ApplicationAnswer> validateAnswers(Long postingId, List<ApplicationRequest.Answer> requested) {
		List<PostingApplicationQuestion> questions = questionRepository
				.findByPostingIdOrderBySortOrderAscQuestionIdAsc(postingId);
		Map<Long, PostingApplicationQuestion> byId = new LinkedHashMap<>();
		questions.forEach(question -> byId.put(question.getQuestionId(), question));

		Set<Long> answered = new HashSet<>();
		List<ApplicationAnswer> result = new java.util.ArrayList<>();
		for (ApplicationRequest.Answer answer : requested == null ? List.<ApplicationRequest.Answer>of() : requested) {
			PostingApplicationQuestion question = byId.get(answer.questionId());
			if (question == null || !answered.add(question.getQuestionId())) {
				throw new BusinessException(ApplicationErrorCode.INVALID_ANSWER);
			}
			List<String> values = answer.values().stream().map(String::trim).toList();
			if (!matches(question, values)) {
				throw new BusinessException(ApplicationErrorCode.INVALID_ANSWER);
			}
			result.add(new ApplicationAnswer(question.getQuestionId(), values));
		}
		for (PostingApplicationQuestion question : questions) {
			if (question.isRequired() && !answered.contains(question.getQuestionId())) {
				throw new BusinessException(ApplicationErrorCode.INVALID_ANSWER);
			}
		}
		return result;
	}

	private boolean matches(PostingApplicationQuestion question, List<String> values) {
		List<String> options = question.getOptions();
		return switch (question.getQuestionType()) {
			case TEXT -> values.size() == 1;
			case SINGLE_CHOICE -> values.size() == 1 && options.contains(values.get(0));
			case MULTI_CHOICE -> !values.isEmpty() && new HashSet<>(values).size() == values.size()
					&& options.containsAll(values);
		};
	}

	private String emptyToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}

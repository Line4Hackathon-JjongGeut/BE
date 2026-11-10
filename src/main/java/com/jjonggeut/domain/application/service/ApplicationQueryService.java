package com.jjonggeut.domain.application.service;

import com.jjonggeut.domain.activity.dto.PageResponse;
import com.jjonggeut.domain.activity.entity.ApplicationAnswer;
import com.jjonggeut.domain.activity.entity.ApplicationStatus;
import com.jjonggeut.domain.activity.entity.VolunteerApplicationRef;
import com.jjonggeut.domain.activity.repository.VolunteerApplicationRefRepository;
import com.jjonggeut.domain.activity.service.PageParams;
import com.jjonggeut.domain.application.dto.ApplicationDetailResponse;
import com.jjonggeut.domain.application.dto.ApplicationListItem;
import com.jjonggeut.domain.application.dto.StatusHistoryItem;
import com.jjonggeut.domain.application.dto.StatusHistoryResponse;
import com.jjonggeut.domain.application.exception.ApplicationErrorCode;
import com.jjonggeut.domain.application.repository.ApplicationStatusHistoryRepository;
import com.jjonggeut.domain.posting.entity.PostingApplicationQuestion;
import com.jjonggeut.domain.posting.entity.PostingSchedule;
import com.jjonggeut.domain.posting.repository.PostingApplicationQuestionRepository;
import com.jjonggeut.domain.posting.repository.PostingScheduleRepository;
import com.jjonggeut.domain.recommendation.entity.PostingRef;
import com.jjonggeut.domain.recommendation.entity.ShelterRef;
import com.jjonggeut.domain.recommendation.repository.PostingRefRepository;
import com.jjonggeut.domain.recommendation.repository.ShelterRefRepository;
import com.jjonggeut.domain.shelter.entity.VerificationStatus;
import com.jjonggeut.global.exception.BusinessException;
import com.jjonggeut.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 내 봉사 신청 목록, 상세, 상태 이력 조회. 남의 신청은 없는 신청과 같게 404 로 응답한다. */
@Service
@Transactional(readOnly = true)
public class ApplicationQueryService {

	private static final String GROUP_ONGOING = "ONGOING";
	private static final String GROUP_HISTORY = "HISTORY";

	private final VolunteerApplicationRefRepository applicationRepository;
	private final ApplicationStatusHistoryRepository historyRepository;
	private final PostingScheduleRepository scheduleRepository;
	private final PostingRefRepository postingRepository;
	private final ShelterRefRepository shelterRepository;
	private final PostingApplicationQuestionRepository questionRepository;

	public ApplicationQueryService(VolunteerApplicationRefRepository applicationRepository,
			ApplicationStatusHistoryRepository historyRepository, PostingScheduleRepository scheduleRepository,
			PostingRefRepository postingRepository, ShelterRefRepository shelterRepository,
			PostingApplicationQuestionRepository questionRepository) {
		this.applicationRepository = applicationRepository;
		this.historyRepository = historyRepository;
		this.scheduleRepository = scheduleRepository;
		this.postingRepository = postingRepository;
		this.shelterRepository = shelterRepository;
		this.questionRepository = questionRepository;
	}

	/** status 는 상태 하나이거나 묶음(ONGOING, HISTORY)이고, 생략하면 전체다. */
	public PageResponse<ApplicationListItem> list(Long userId, String status, Integer page, Integer size) {
		List<ApplicationStatus> statuses = parseStatuses(status);
		Page<Object[]> result = applicationRepository.findMine(userId, statuses, PageParams.of(page, size));
		LocalDateTime now = LocalDateTime.now();
		return PageResponse.of(result, row -> {
			VolunteerApplicationRef application = (VolunteerApplicationRef) row[0];
			PostingRef posting = (PostingRef) row[1];
			ShelterRef shelter = (ShelterRef) row[2];
			PostingSchedule schedule = (PostingSchedule) row[3];
			LocalDateTime startAt = ApplicationPolicy.startAt(schedule, posting);
			return new ApplicationListItem(application.getApplicationId(), application.getStatus(),
					posting.getPostingId(), posting.getTitle(),
					new ApplicationListItem.Shelter(shelter.getShelterId(), shelter.getName()),
					application.getScheduleId(), startAt, ApplicationPolicy.endAt(schedule, posting),
					application.getAppliedAt(), ApplicationPolicy.cancellable(application, startAt, now));
		});
	}

	public ApplicationDetailResponse detail(Long userId, Long applicationId) {
		VolunteerApplicationRef application = requireMine(userId, applicationId);
		PostingRef posting = postingRepository.findById(application.getPostingId())
				.orElseThrow(() -> new BusinessException(ApplicationErrorCode.APPLICATION_NOT_FOUND));
		ShelterRef shelter = shelterRepository.findById(posting.getShelterId())
				.orElseThrow(() -> new BusinessException(ApplicationErrorCode.APPLICATION_NOT_FOUND));
		PostingSchedule schedule = application.getScheduleId() == null ? null
				: scheduleRepository.findById(application.getScheduleId()).orElse(null);

		LocalDateTime startAt = ApplicationPolicy.startAt(schedule, posting);
		LocalDateTime endAt = ApplicationPolicy.endAt(schedule, posting);
		ApplicationDetailResponse.ScheduleInfo scheduleInfo = schedule != null
				? new ApplicationDetailResponse.ScheduleInfo(schedule.getScheduleId(), startAt, endAt)
				: startAt != null ? new ApplicationDetailResponse.ScheduleInfo(null, startAt, endAt) : null;
		boolean verified = shelter.getVerificationStatus() == VerificationStatus.VERIFIED;

		return new ApplicationDetailResponse(application.getApplicationId(), application.getStatus(),
				application.getAppliedAt(), application.getMessage(), answers(application, posting),
				new ApplicationDetailResponse.PostingInfo(posting.getPostingId(), posting.getTitle(),
						posting.getPostingType(), posting.getStatus()),
				new ApplicationDetailResponse.ShelterInfo(shelter.getShelterId(), shelter.getName(),
						shelter.getAddress(), verified ? shelter.getPhone() : null),
				scheduleInfo, posting.getPreparations(),
				ApplicationPolicy.cancellable(application, startAt, LocalDateTime.now()),
				ApplicationPolicy.cancelDeadline(startAt), historyItems(applicationId));
	}

	public StatusHistoryResponse statusHistory(Long userId, Long applicationId) {
		VolunteerApplicationRef application = requireMine(userId, applicationId);
		return new StatusHistoryResponse(applicationId, application.getStatus(), historyItems(applicationId));
	}

	private VolunteerApplicationRef requireMine(Long userId, Long applicationId) {
		if (applicationId == null || applicationId < 1) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		return applicationRepository.findByApplicationIdAndUserId(applicationId, userId)
				.orElseThrow(() -> new BusinessException(ApplicationErrorCode.APPLICATION_NOT_FOUND));
	}

	private List<StatusHistoryItem> historyItems(Long applicationId) {
		return historyRepository.findByApplicationIdOrderByChangedAtAscHistoryIdAsc(applicationId).stream()
				.map(StatusHistoryItem::from)
				.toList();
	}

	/** 질문이 표시되는 순서대로, 답한 질문만 라벨과 함께 보여 준다. */
	private List<ApplicationDetailResponse.AnswerItem> answers(VolunteerApplicationRef application, PostingRef posting) {
		Map<Long, ApplicationAnswer> byQuestion = application.getAnswers().stream()
				.collect(Collectors.toMap(ApplicationAnswer::questionId, Function.identity(), (a, b) -> a));
		List<ApplicationDetailResponse.AnswerItem> items = new ArrayList<>();
		for (PostingApplicationQuestion question : questionRepository
				.findByPostingIdOrderBySortOrderAscQuestionIdAsc(posting.getPostingId())) {
			ApplicationAnswer answer = byQuestion.get(question.getQuestionId());
			if (answer != null) {
				items.add(new ApplicationDetailResponse.AnswerItem(question.getQuestionId(), question.getLabel(),
						answer.values()));
			}
		}
		return items;
	}

	private List<ApplicationStatus> parseStatuses(String status) {
		if (status == null) {
			return Arrays.asList(ApplicationStatus.values());
		}
		if (GROUP_ONGOING.equals(status)) {
			return ApplicationStatus.ONGOING;
		}
		if (GROUP_HISTORY.equals(status)) {
			return ApplicationStatus.HISTORY;
		}
		return Arrays.stream(ApplicationStatus.values())
				.filter(candidate -> candidate.name().equals(status))
				.findFirst()
				.map(List::of)
				.orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED));
	}
}

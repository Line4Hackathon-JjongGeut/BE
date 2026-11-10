package com.jjonggeut.domain.activity.service;

import com.jjonggeut.domain.activity.dto.ActivityCalendarResponse;
import com.jjonggeut.domain.activity.dto.ActivitySummaryResponse;
import com.jjonggeut.domain.activity.dto.PageResponse;
import com.jjonggeut.domain.activity.dto.ParticipationHistoryItem;
import com.jjonggeut.domain.activity.entity.ApplicationStatus;
import com.jjonggeut.domain.activity.entity.VolunteerApplicationRef;
import com.jjonggeut.domain.activity.repository.NotificationRefRepository;
import com.jjonggeut.domain.activity.repository.PostingBookmarkRefRepository;
import com.jjonggeut.domain.activity.repository.VolunteerApplicationRefRepository;
import com.jjonggeut.domain.recommendation.entity.PostingRef;
import com.jjonggeut.domain.recommendation.entity.ShelterRef;
import com.jjonggeut.global.exception.BusinessException;
import com.jjonggeut.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 내 활동 요약, 활동 캘린더, 참여 이력 조회. */
@Service
@Transactional(readOnly = true)
public class ActivityQueryService {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final Pattern MONTH_FORMAT = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

	private final PostingBookmarkRefRepository postingBookmarkRepository;
	private final VolunteerApplicationRefRepository applicationRepository;
	private final NotificationRefRepository notificationRepository;

	public ActivityQueryService(PostingBookmarkRefRepository postingBookmarkRepository,
			VolunteerApplicationRefRepository applicationRepository,
			NotificationRefRepository notificationRepository) {
		this.postingBookmarkRepository = postingBookmarkRepository;
		this.applicationRepository = applicationRepository;
		this.notificationRepository = notificationRepository;
	}

	public ActivitySummaryResponse getSummary(Long userId) {
		return new ActivitySummaryResponse(
				postingBookmarkRepository.countByUserId(userId),
				applicationRepository.countByUserIdAndStatusIn(userId, ApplicationStatus.ONGOING),
				applicationRepository.countByUserIdAndStatusIn(userId, List.of(ApplicationStatus.COMPLETED)),
				notificationRepository.countByUserIdAndReadAtIsNull(userId));
	}

	/** month 를 생략하면 이번 달(한국 시간)이다. */
	public ActivityCalendarResponse getCalendar(Long userId, String month) {
		YearMonth target = parseMonth(month);
		LocalDateTime from = target.atDay(1).atStartOfDay();
		LocalDateTime to = target.plusMonths(1).atDay(1).atStartOfDay();

		List<ActivityCalendarResponse.Item> items = applicationRepository
				.findCalendar(userId, ApplicationStatus.CALENDAR, from, to).stream()
				.map(row -> {
					VolunteerApplicationRef application = (VolunteerApplicationRef) row[0];
					PostingRef posting = (PostingRef) row[1];
					ShelterRef shelter = (ShelterRef) row[2];
					return new ActivityCalendarResponse.Item(application.getApplicationId(), application.getStatus(),
							posting.getPostingId(), posting.getTitle(), shelter.getShelterId(), shelter.getName(),
							posting.getActivityStartAt());
				})
				.toList();
		return new ActivityCalendarResponse(target.toString(), items);
	}

	/** status 를 생략하면 끝난 상태 전체이고, 진행 중 상태나 알 수 없는 값은 400 이다. */
	public PageResponse<ParticipationHistoryItem> getHistory(Long userId, String status, Integer page,
			Integer size) {
		List<ApplicationStatus> statuses = parseHistoryStatus(status);
		Page<Object[]> result = applicationRepository.findHistory(userId, statuses,
				PageParams.of(page, size));
		return PageResponse.of(result, row -> {
			VolunteerApplicationRef application = (VolunteerApplicationRef) row[0];
			PostingRef posting = (PostingRef) row[1];
			ShelterRef shelter = (ShelterRef) row[2];
			return new ParticipationHistoryItem(application.getApplicationId(), application.getStatus(),
					posting.getPostingId(), posting.getTitle(), shelter.getShelterId(), shelter.getName(),
					posting.getActivityStartAt(), application.getAppliedAt());
		});
	}

	private YearMonth parseMonth(String month) {
		if (month == null) {
			return YearMonth.now(SEOUL);
		}
		if (!MONTH_FORMAT.matcher(month).matches()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		try {
			return YearMonth.parse(month);
		} catch (DateTimeParseException e) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
	}

	private List<ApplicationStatus> parseHistoryStatus(String status) {
		if (status == null) {
			return ApplicationStatus.HISTORY;
		}
		return ApplicationStatus.HISTORY.stream()
				.filter(candidate -> candidate.name().equals(status))
				.findFirst()
				.map(List::of)
				.orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED));
	}
}

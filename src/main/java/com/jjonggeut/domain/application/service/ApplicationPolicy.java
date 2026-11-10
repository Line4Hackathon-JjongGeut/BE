package com.jjonggeut.domain.application.service;

import com.jjonggeut.domain.activity.entity.ApplicationStatus;
import com.jjonggeut.domain.activity.entity.VolunteerApplicationRef;
import com.jjonggeut.domain.posting.entity.PostingSchedule;
import com.jjonggeut.domain.recommendation.entity.PostingRef;
import java.time.Duration;
import java.time.LocalDateTime;

/** 신청 취소 가능 여부 같은 규칙. 일정 시작 24시간 전까지만 취소할 수 있다. */
final class ApplicationPolicy {

	static final Duration CANCEL_BEFORE = Duration.ofHours(24);

	private ApplicationPolicy() {
	}

	/** 신청한 일정의 시작. 일정이 없는 예전 신청은 공고의 활동 시작을 쓰고, 그것도 없으면 null 이다. */
	static LocalDateTime startAt(PostingSchedule schedule, PostingRef posting) {
		if (schedule != null) {
			return schedule.getStartAt();
		}
		return posting == null ? null : posting.getActivityStartAt();
	}

	static LocalDateTime endAt(PostingSchedule schedule, PostingRef posting) {
		if (schedule != null) {
			return schedule.getEndAt();
		}
		return posting == null ? null : posting.getActivityEndAt();
	}

	static LocalDateTime cancelDeadline(LocalDateTime startAt) {
		return startAt == null ? null : startAt.minus(CANCEL_BEFORE);
	}

	static boolean beforeDeadline(LocalDateTime startAt, LocalDateTime now) {
		return startAt == null || !now.isAfter(cancelDeadline(startAt));
	}

	static boolean cancellable(VolunteerApplicationRef application, LocalDateTime startAt, LocalDateTime now) {
		return ApplicationStatus.ONGOING.contains(application.getStatus()) && beforeDeadline(startAt, now);
	}
}

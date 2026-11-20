package com.jjonggeut.domain.application.dto;

import com.jjonggeut.domain.activity.entity.ApplicationStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ApplicationDetailResponse(
		Long applicationId,
		ApplicationStatus status,
		LocalDateTime appliedAt,
		String message,
		List<AnswerItem> answers,
		PostingInfo posting,
		ShelterInfo shelter,
		ScheduleInfo schedule,
		List<String> preparations,
		boolean cancellable,
		LocalDateTime cancelDeadlineAt,
		List<StatusHistoryItem> statusHistory,
		ExperienceInfo experience) {

	public record AnswerItem(Long questionId, String label, List<String> values) {
	}

	/** 신청 당시 첨부한 경험 스냅샷. experienceId 는 원본이 삭제되면 null 이다. */
	public record ExperienceInfo(Long experienceId, String snapshot) {
	}

	public record PostingInfo(Long postingId, String title, String postingType, String status) {
	}

	/** 연락처는 인증된 시설만 보여 준다. */
	public record ShelterInfo(Long shelterId, String name, String address, String phone) {
	}

	/** scheduleId 는 일정 도입 전의 예전 신청이면 null 이다. */
	public record ScheduleInfo(Long scheduleId, LocalDateTime startAt, LocalDateTime endAt) {
	}
}

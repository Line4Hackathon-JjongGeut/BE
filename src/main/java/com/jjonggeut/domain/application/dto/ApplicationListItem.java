package com.jjonggeut.domain.application.dto;

import com.jjonggeut.domain.activity.entity.ApplicationStatus;
import java.time.LocalDateTime;

public record ApplicationListItem(
		Long applicationId,
		ApplicationStatus status,
		Long postingId,
		String title,
		Shelter shelter,
		Long scheduleId,
		LocalDateTime activityStartAt,
		LocalDateTime activityEndAt,
		LocalDateTime appliedAt,
		boolean cancellable) {

	public record Shelter(Long shelterId, String name) {
	}
}

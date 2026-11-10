package com.jjonggeut.domain.application.dto;

import com.jjonggeut.domain.activity.entity.ApplicationStatus;
import java.time.LocalDateTime;

public record ApplicationResponse(
		Long applicationId,
		Long scheduleId,
		Long postingId,
		ApplicationStatus status,
		LocalDateTime activityStartAt,
		LocalDateTime activityEndAt,
		LocalDateTime appliedAt) {
}

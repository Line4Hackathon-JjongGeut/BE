package com.jjonggeut.domain.activity.dto;

import com.jjonggeut.domain.activity.entity.ApplicationStatus;
import java.time.LocalDateTime;

public record ParticipationHistoryItem(
		Long applicationId,
		ApplicationStatus status,
		Long postingId,
		String postingTitle,
		Long shelterId,
		String shelterName,
		LocalDateTime activityStartAt,
		LocalDateTime appliedAt
) {
}

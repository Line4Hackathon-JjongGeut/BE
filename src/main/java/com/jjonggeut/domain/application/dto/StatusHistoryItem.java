package com.jjonggeut.domain.application.dto;

import com.jjonggeut.domain.activity.entity.ApplicationStatus;
import com.jjonggeut.domain.application.entity.ApplicationStatusHistory;
import com.jjonggeut.domain.application.entity.StatusActor;
import java.time.LocalDateTime;

public record StatusHistoryItem(
		Long historyId,
		ApplicationStatus fromStatus,
		ApplicationStatus toStatus,
		StatusActor actor,
		String reason,
		LocalDateTime changedAt) {

	public static StatusHistoryItem from(ApplicationStatusHistory history) {
		return new StatusHistoryItem(history.getHistoryId(), history.getFromStatus(), history.getToStatus(),
				history.getActor(), history.getReason(), history.getChangedAt());
	}
}

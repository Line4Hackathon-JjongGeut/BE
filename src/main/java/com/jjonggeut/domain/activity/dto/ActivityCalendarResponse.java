package com.jjonggeut.domain.activity.dto;

import com.jjonggeut.domain.activity.entity.ApplicationStatus;
import java.time.LocalDateTime;
import java.util.List;

/** month 는 YYYY-MM, items 는 활동 시작이 빠른 순서다. */
public record ActivityCalendarResponse(String month, List<Item> items) {

	public record Item(
			Long applicationId,
			ApplicationStatus status,
			Long postingId,
			String postingTitle,
			Long shelterId,
			String shelterName,
			LocalDateTime activityStartAt
	) {
	}
}

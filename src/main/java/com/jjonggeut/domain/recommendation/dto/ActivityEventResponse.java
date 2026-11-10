package com.jjonggeut.domain.recommendation.dto;

import com.jjonggeut.domain.recommendation.entity.ActivityEventType;
import com.jjonggeut.domain.recommendation.entity.UserActivityEvent;
import java.time.LocalDateTime;

public record ActivityEventResponse(
		Long activityEventId,
		ActivityEventType eventType,
		Long postingId,
		Long shelterId,
		Long recommendationItemId,
		LocalDateTime occurredAt
) {

	public static ActivityEventResponse from(UserActivityEvent event) {
		return new ActivityEventResponse(event.getActivityEventId(), event.getEventType(), event.getPostingId(),
				event.getShelterId(), event.getRecommendationItemId(), event.getOccurredAt());
	}
}

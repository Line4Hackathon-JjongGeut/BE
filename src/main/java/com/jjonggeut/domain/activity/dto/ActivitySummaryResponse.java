package com.jjonggeut.domain.activity.dto;

public record ActivitySummaryResponse(
		long bookmarkedPostingCount,
		long ongoingApplicationCount,
		long completedParticipationCount,
		long unreadNotificationCount
) {
}

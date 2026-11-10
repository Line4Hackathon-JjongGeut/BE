package com.jjonggeut.domain.activity.dto;

import java.time.LocalDateTime;

public record BookmarkedShelterItem(
		Long shelterId,
		String name,
		RegionResponse region,
		long openPostingCount,
		LocalDateTime bookmarkedAt
) {
}

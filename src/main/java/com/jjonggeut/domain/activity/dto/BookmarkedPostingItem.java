package com.jjonggeut.domain.activity.dto;

import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import java.time.LocalDateTime;
import java.util.List;

/** status 는 모집 상태(OPEN / CLOSED)다. */
public record BookmarkedPostingItem(
		Long postingId,
		String postingType,
		String title,
		String status,
		Long shelterId,
		String shelterName,
		RegionResponse region,
		List<ActivityCategory> activityCategories,
		String animalType,
		LocalDateTime activityStartAt,
		Integer capacity,
		Integer applicantCount,
		LocalDateTime bookmarkedAt
) {
}

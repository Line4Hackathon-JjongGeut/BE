package com.jjonggeut.domain.shelter.dto;

import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import java.time.LocalDateTime;
import java.util.List;

/** 시설 모집공고 목록의 한 항목. 모집 인원이 없으면 remainingCount 도 null 이다. */
public record ShelterPostingItem(
		Long postingId,
		String postingType,
		String title,
		List<ActivityCategory> activityCategories,
		String animalType,
		LocalDateTime activityStartAt,
		Integer capacity,
		Integer applicantCount,
		Integer remainingCount,
		boolean bookmarked,
		String thumbnailUrl) {
}

package com.jjonggeut.domain.posting.dto;

import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import java.time.LocalDateTime;
import java.util.List;

/** 검색·긴급 조회가 돌려주는 공고 한 건. 모집 인원이 없으면 remainingCount 도 null 이다. */
public record PostingSummaryItem(
		Long postingId,
		String postingType,
		String title,
		String status,
		boolean urgent,
		PostingShelterSummary shelter,
		List<ActivityCategory> activityCategories,
		String animalType,
		LocalDateTime activityStartAt,
		LocalDateTime recruitEndAt,
		Integer capacity,
		Integer applicantCount,
		Integer remainingCount,
		boolean beginnerFriendly,
		Double distanceKm,
		boolean bookmarked) {
}

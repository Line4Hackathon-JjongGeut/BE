package com.jjonggeut.domain.savedsearch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jjonggeut.domain.profile.entity.AvailableDay;
import com.jjonggeut.domain.profile.entity.AvailableTimeSlot;
import com.jjonggeut.domain.savedsearch.entity.SavedSearch;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import java.time.LocalDateTime;
import java.util.List;

/** 저장된 검색 조건. 지정하지 않은 단일 값은 null, 배열은 빈 배열이다. */
public record SavedSearchResponse(
		Long savedSearchId,
		String name,
		Region region,
		Integer maxDistanceKm,
		List<AvailableDay> availableDays,
		List<AvailableTimeSlot> availableTimeSlots,
		List<ActivityCategory> activityCategories,
		@JsonProperty("isDefault")
		boolean isDefault,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {

	public record Region(String sido, String sigungu, Double latitude, Double longitude) {
	}

	public static SavedSearchResponse from(SavedSearch search) {
		boolean hasRegion = search.getRegionSido() != null || search.getRegionSigungu() != null
				|| search.getLatitude() != null || search.getLongitude() != null;
		Region region = hasRegion
				? new Region(search.getRegionSido(), search.getRegionSigungu(), search.getLatitude(),
						search.getLongitude())
				: null;
		return new SavedSearchResponse(search.getSavedSearchId(), search.getName(), region,
				search.getMaxDistanceKm(), search.getAvailableDays(), search.getAvailableTimeSlots(),
				search.getActivityCategories(), search.isDefaultSearch(), search.getCreatedAt(),
				search.getUpdatedAt());
	}
}

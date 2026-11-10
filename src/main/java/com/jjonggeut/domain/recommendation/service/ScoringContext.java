package com.jjonggeut.domain.recommendation.service;

import com.jjonggeut.domain.profile.entity.AvailableDay;
import com.jjonggeut.domain.profile.entity.AvailableTimeSlot;
import com.jjonggeut.domain.profile.entity.InterestAnimal;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import java.util.Set;

/**
 * 추천 점수 계산에 쓰는 사용자 조건. 이름 등 개인 식별 정보는 담지 않는다.
 * latitude/longitude 가 null 이면 거리를 반영하지 않는다.
 */
public record ScoringContext(
		Double latitude,
		Double longitude,
		Integer maxDistanceKm,
		Set<ActivityCategory> interestCategories,
		Set<ActivityCategory> historyCategories,
		Set<InterestAnimal> interestAnimals,
		Set<AvailableDay> availableDays,
		Set<AvailableTimeSlot> availableTimeSlots
) {

	public boolean hasLocation() {
		return latitude != null && longitude != null;
	}

	public static ScoringContext empty() {
		return new ScoringContext(null, null, null, Set.of(), Set.of(), Set.of(), Set.of(), Set.of());
	}
}

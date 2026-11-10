package com.jjonggeut.domain.profile.dto;

import com.jjonggeut.domain.profile.entity.ActivityFrequency;
import com.jjonggeut.domain.profile.entity.AvailableDay;
import com.jjonggeut.domain.profile.entity.AvailableTimeSlot;
import com.jjonggeut.domain.profile.entity.ExperienceLevel;
import com.jjonggeut.domain.profile.entity.InterestAnimal;
import com.jjonggeut.domain.profile.entity.UserProfile;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import java.util.List;

/** 내 추천 프로필. 프로필이 없으면 onboardingCompleted=false, 단일 값은 null, 배열은 빈 배열이다. */
public record ProfileResponse(
		boolean onboardingCompleted,
		Region region,
		Integer maxDistanceKm,
		ExperienceLevel experienceLevel,
		ActivityFrequency activityFrequency,
		List<InterestAnimal> interestAnimals,
		List<ActivityCategory> interestCategories,
		List<AvailableDay> availableDays,
		List<AvailableTimeSlot> availableTimeSlots
) {

	public record Region(String sido, String sigungu, Double latitude, Double longitude) {
	}

	public static ProfileResponse empty() {
		return new ProfileResponse(false, null, null, null, null, List.of(), List.of(), List.of(), List.of());
	}

	public static ProfileResponse from(UserProfile profile) {
		boolean hasRegion = profile.getRegionSido() != null || profile.getRegionSigungu() != null
				|| profile.getLatitude() != null || profile.getLongitude() != null;
		Region region = hasRegion
				? new Region(profile.getRegionSido(), profile.getRegionSigungu(), profile.getLatitude(),
						profile.getLongitude())
				: null;
		return new ProfileResponse(
				true,
				region,
				profile.getMaxDistanceKm(),
				profile.getExperienceLevel(),
				profile.getActivityFrequency(),
				profile.getInterestAnimals(),
				profile.getInterestCategories(),
				profile.getAvailableDays(),
				profile.getAvailableTimeSlots());
	}
}

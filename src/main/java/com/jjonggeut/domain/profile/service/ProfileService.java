package com.jjonggeut.domain.profile.service;

import com.jjonggeut.domain.profile.dto.ProfileResponse;
import com.jjonggeut.domain.profile.dto.ProfileUpdateRequest;
import com.jjonggeut.domain.profile.entity.UserProfile;
import com.jjonggeut.domain.profile.repository.UserProfileRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

	private final UserProfileRepository profileRepository;

	public ProfileService(UserProfileRepository profileRepository) {
		this.profileRepository = profileRepository;
	}

	/** 프로필이 아직 없으면(온보딩 전) 404 가 아니라 빈 프로필로 응답한다. */
	@Transactional(readOnly = true)
	public ProfileResponse getMyProfile(Long userId) {
		return profileRepository.findByUserId(userId)
				.map(ProfileResponse::from)
				.orElseGet(ProfileResponse::empty);
	}

	/** 프로필이 없으면 새로 만들고, 있으면 보낸 항목만 수정한다. 보내지 않은(null) 항목은 그대로 둔다. */
	@Transactional
	public ProfileResponse updateMyProfile(Long userId, ProfileUpdateRequest request) {
		UserProfile profile = profileRepository.findByUserId(userId)
				.orElseGet(() -> profileRepository.save(UserProfile.empty(userId)));

		if (request.region() != null) {
			ProfileUpdateRequest.RegionRequest region = request.region();
			profile.changeRegion(region.sido().trim(), region.sigungu().trim(), region.latitude(),
					region.longitude());
		}
		if (request.maxDistanceKm() != null) {
			profile.changeMaxDistanceKm(request.maxDistanceKm());
		}
		if (request.experienceLevel() != null) {
			profile.changeExperienceLevel(request.experienceLevel());
		}
		if (request.activityFrequency() != null) {
			profile.changeActivityFrequency(request.activityFrequency());
		}
		if (request.interestAnimals() != null) {
			profile.changeInterestAnimals(distinct(request.interestAnimals()));
		}
		if (request.interestCategories() != null) {
			profile.changeInterestCategories(distinct(request.interestCategories()));
		}
		if (request.availableDays() != null) {
			profile.changeAvailableDays(distinct(request.availableDays()));
		}
		if (request.availableTimeSlots() != null) {
			profile.changeAvailableTimeSlots(distinct(request.availableTimeSlots()));
		}
		return ProfileResponse.from(profile);
	}

	/** null 요소와 중복을 제거하고 입력 순서를 유지한다. */
	private <T> List<T> distinct(List<T> values) {
		return List.copyOf(new LinkedHashSet<>(values.stream().filter(Objects::nonNull).toList()));
	}
}

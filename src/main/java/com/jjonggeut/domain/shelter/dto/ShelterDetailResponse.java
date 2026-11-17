package com.jjonggeut.domain.shelter.dto;

import com.jjonggeut.domain.activity.dto.RegionResponse;
import com.jjonggeut.domain.profile.entity.InterestAnimal;
import com.jjonggeut.domain.shelter.entity.VerificationStatus;
import java.util.List;

/** 보호시설 상세. 연락처는 인증된 시설만 내려가고, 미인증 시설은 null 이다. */
public record ShelterDetailResponse(
		Long shelterId,
		String name,
		RegionResponse region,
		String address,
		Double latitude,
		Double longitude,
		String phone,
		String description,
		List<InterestAnimal> animalTypes,
		VerificationStatus verificationStatus,
		Double distanceKm,
		boolean bookmarked,
		long openPostingCount,
		List<ShelterOpenPostingItem> openPostings,
		List<String> imageUrls) {
}

package com.jjonggeut.domain.shelter.dto;

import com.jjonggeut.domain.activity.dto.RegionResponse;
import com.jjonggeut.domain.profile.entity.InterestAnimal;
import com.jjonggeut.domain.shelter.entity.VerificationStatus;
import java.util.List;

/** distanceKm 는 좌표를 보낸 경우에만(시설 좌표가 있을 때) 채운다. */
public record ShelterListItem(
		Long shelterId,
		String name,
		RegionResponse region,
		String address,
		Double latitude,
		Double longitude,
		String phone,
		List<InterestAnimal> animalTypes,
		VerificationStatus verificationStatus,
		Double distanceKm,
		long openPostingCount,
		boolean bookmarked,
		String thumbnailUrl
) {
}

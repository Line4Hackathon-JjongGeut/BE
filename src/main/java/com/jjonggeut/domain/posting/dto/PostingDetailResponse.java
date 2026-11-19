package com.jjonggeut.domain.posting.dto;

import com.jjonggeut.domain.activity.dto.RegionResponse;
import com.jjonggeut.domain.posting.entity.FosterContactMethod;
import com.jjonggeut.domain.shelter.entity.VerificationStatus;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import java.time.LocalDateTime;
import java.util.List;

/** 공고 상세. 공고 종류에 맞는 블록(volunteer, sponsor, foster)만 채워지고 나머지는 null 이다. */
public record PostingDetailResponse(
		Long postingId,
		String postingType,
		String title,
		String status,
		boolean urgent,
		String description,
		String animalType,
		LocalDateTime recruitEndAt,
		LocalDateTime createdAt,
		ShelterInfo shelter,
		Double distanceKm,
		boolean bookmarked,
		VolunteerInfo volunteer,
		SponsorInfo sponsor,
		FosterInfo foster,
		List<String> imageUrls) {

	/** 연락처(phone)는 인증된 시설만 내려간다. */
	public record ShelterInfo(
			Long shelterId,
			String name,
			RegionResponse region,
			String address,
			Double latitude,
			Double longitude,
			String phone,
			VerificationStatus verificationStatus) {
	}

	public record VolunteerInfo(
			List<ActivityCategory> activityCategories,
			LocalDateTime activityStartAt,
			LocalDateTime activityEndAt,
			Integer capacity,
			Integer applicantCount,
			Integer remainingCount,
			Conditions conditions) {
	}

	public record Conditions(Integer minAge, boolean beginnerFriendly, String requirements) {
	}

	/** targetQuantity 가 없으면(수량 집계 안 하는 공고) progressRate 도 null 이다. 필요 물품이 없으면 neededItems 는 빈 목록이다. */
	public record SponsorInfo(String donationPurpose, boolean donationLinkAvailable, Integer targetQuantity,
			Integer receivedQuantity, Integer progressRate, List<String> neededItems) {
	}

	public record FosterInfo(FosterContactMethod contactMethod, String externalUrl, String petName,
			String petAgeText, String petGender, String petCharacteristics) {
	}
}

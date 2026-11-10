package com.jjonggeut.domain.profile.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jjonggeut.domain.profile.entity.ActivityFrequency;
import com.jjonggeut.domain.profile.entity.AvailableDay;
import com.jjonggeut.domain.profile.entity.AvailableTimeSlot;
import com.jjonggeut.domain.profile.entity.ExperienceLevel;
import com.jjonggeut.domain.profile.entity.InterestAnimal;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 내 추천 프로필 수정 요청. 보내지 않았거나 null 인 필드는 변경하지 않는다.
 * 배열은 빈 배열([])을 보내면 비우고, region 은 보내면 통째로 교체한다.
 */
public record ProfileUpdateRequest(

		@Valid
		RegionRequest region,

		@Min(value = 1, message = "이동 가능 거리는 1km 이상이어야 합니다.")
		@Max(value = 200, message = "이동 가능 거리는 200km 이하여야 합니다.")
		Integer maxDistanceKm,

		ExperienceLevel experienceLevel,

		ActivityFrequency activityFrequency,

		@Size(max = 2, message = "관심 동물은 최대 2개까지 선택할 수 있습니다.")
		List<InterestAnimal> interestAnimals,

		@Size(max = 7, message = "관심 활동은 최대 7개까지 선택할 수 있습니다.")
		List<ActivityCategory> interestCategories,

		@Size(max = 7, message = "가능 요일은 최대 7개까지 선택할 수 있습니다.")
		List<AvailableDay> availableDays,

		@Size(max = 3, message = "가능 시간대는 최대 3개까지 선택할 수 있습니다.")
		List<AvailableTimeSlot> availableTimeSlots
) {

	/** 수정할 항목이 하나도 없는 요청은 허용하지 않는다. */
	@JsonIgnore
	@AssertTrue(message = "수정할 항목을 1개 이상 입력해주세요.")
	public boolean isAnyFieldPresent() {
		return region != null || maxDistanceKm != null || experienceLevel != null || activityFrequency != null
				|| interestAnimals != null || interestCategories != null || availableDays != null
				|| availableTimeSlots != null;
	}

	public record RegionRequest(

			@NotBlank(message = "시/도를 입력해주세요.")
			@Size(max = 50, message = "시/도는 50자 이하로 입력해주세요.")
			String sido,

			@NotBlank(message = "시/군/구를 입력해주세요.")
			@Size(max = 50, message = "시/군/구는 50자 이하로 입력해주세요.")
			String sigungu,

			@DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
			@DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
			Double latitude,

			@DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
			@DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
			Double longitude
	) {

		/** 위도와 경도는 함께 입력하거나 둘 다 생략해야 한다. */
		@JsonIgnore
		@AssertTrue(message = "위도와 경도는 함께 입력해주세요.")
		public boolean isCoordinatesPaired() {
			return (latitude == null) == (longitude == null);
		}
	}
}

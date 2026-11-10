package com.jjonggeut.domain.savedsearch.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jjonggeut.domain.profile.entity.AvailableDay;
import com.jjonggeut.domain.profile.entity.AvailableTimeSlot;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 검색 조건 저장 요청. 지역·거리·요일·시간·활동 유형 중 하나 이상을 지정해야 한다.
 * isDefault 를 생략하면 false 이고, true 로 저장하면 기존 기본 조건은 해제된다.
 */
public record SavedSearchCreateRequest(

		@NotBlank(message = "검색 조건 이름을 입력해주세요.")
		@Size(max = 20, message = "검색 조건 이름은 20자 이하로 입력해주세요.")
		String name,

		@Valid
		SavedSearchRegionRequest region,

		@Min(value = 1, message = "거리는 1km 이상이어야 합니다.")
		@Max(value = 200, message = "거리는 200km 이하여야 합니다.")
		Integer maxDistanceKm,

		@Size(max = 7, message = "요일은 최대 7개까지 선택할 수 있습니다.")
		List<AvailableDay> availableDays,

		@Size(max = 3, message = "시간대는 최대 3개까지 선택할 수 있습니다.")
		List<AvailableTimeSlot> availableTimeSlots,

		@Size(max = 7, message = "활동 유형은 최대 7개까지 선택할 수 있습니다.")
		List<ActivityCategory> activityCategories,

		@JsonProperty("isDefault")
		Boolean isDefault
) {

	/** 빈 조건은 저장할 이유가 없다. */
	@JsonIgnore
	@AssertTrue(message = "지역, 거리, 요일, 시간대, 활동 유형 중 하나 이상을 지정해주세요.")
	public boolean isAnyFilterPresent() {
		return region != null || maxDistanceKm != null
				|| (availableDays != null && !availableDays.isEmpty())
				|| (availableTimeSlots != null && !availableTimeSlots.isEmpty())
				|| (activityCategories != null && !activityCategories.isEmpty());
	}
}

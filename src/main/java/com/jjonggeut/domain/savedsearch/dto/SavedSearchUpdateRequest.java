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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 저장 검색 조건 수정 요청. 보낸 항목만 바뀌고 생략하거나 null 인 항목은 그대로다.
 * 배열은 []로 비울 수 있고, region 은 보내면 통째로 교체한다.
 * 수정한 결과에 지정된 조건이 하나도 남지 않으면 허용하지 않는다.
 */
public record SavedSearchUpdateRequest(

		@Pattern(regexp = "^\\S.*$", message = "검색 조건 이름은 공백으로 시작할 수 없습니다.")
		@Size(min = 1, max = 20, message = "검색 조건 이름은 1~20자로 입력해주세요.")
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

	@JsonIgnore
	@AssertTrue(message = "수정할 항목을 1개 이상 입력해주세요.")
	public boolean isAnyFieldPresent() {
		return name != null || region != null || maxDistanceKm != null || availableDays != null
				|| availableTimeSlots != null || activityCategories != null || isDefault != null;
	}
}

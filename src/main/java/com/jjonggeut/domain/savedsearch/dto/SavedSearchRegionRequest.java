package com.jjonggeut.domain.savedsearch.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 검색 지역. 프로필의 지역과 같은 형식이며 거리 기준이 되는 좌표를 함께 저장할 수 있다. */
public record SavedSearchRegionRequest(

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

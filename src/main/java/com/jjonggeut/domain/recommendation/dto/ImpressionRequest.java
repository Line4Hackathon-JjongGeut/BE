package com.jjonggeut.domain.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;

/** 추천 노출 기록 요청. itemIds 를 생략하면 묶음 전체를 노출로 기록한다. */
public record ImpressionRequest(

		@Size(min = 1, max = 20, message = "노출 항목은 1~20개까지 보낼 수 있습니다.")
		List<Long> itemIds
) {

	/** null 이거나 중복된 항목 ID 는 허용하지 않는다. */
	@JsonIgnore
	@AssertTrue(message = "항목 ID 에 null 이나 중복이 있습니다.")
	public boolean isItemIdsValid() {
		if (itemIds == null) {
			return true;
		}
		return itemIds.stream().noneMatch(Objects::isNull) && itemIds.size() == itemIds.stream().distinct().count();
	}
}

package com.jjonggeut.domain.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jjonggeut.domain.recommendation.entity.ActivityEventType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

/**
 * 사용자 행동 이벤트 기록 요청.
 * 공고 이벤트(POSTING_VIEW/BOOKMARK/APPLY)는 postingId, SHELTER_VIEW 는 shelterId,
 * DONATION_LINK_CLICK 은 postingId 또는 shelterId 가 필요하다.
 * occurredAt 을 생략하면 서버 시각으로 기록한다.
 */
public record ActivityEventRequest(

		@NotNull(message = "이벤트 종류는 필수입니다.")
		ActivityEventType eventType,

		@Positive(message = "공고 ID 는 양수여야 합니다.")
		Long postingId,

		@Positive(message = "시설 ID 는 양수여야 합니다.")
		Long shelterId,

		@Positive(message = "추천 항목 ID 는 양수여야 합니다.")
		Long recommendationItemId,

		LocalDateTime occurredAt
) {

	/** 이벤트 종류에 맞는 대상 ID 가 있어야 한다. eventType 이 비어 있으면 @NotNull 이 먼저 걸러낸다. */
	@JsonIgnore
	@AssertTrue(message = "이벤트 종류에 맞는 공고 ID 또는 시설 ID 가 필요합니다.")
	public boolean isTargetPresent() {
		if (eventType == null) {
			return true;
		}
		return switch (eventType) {
			case POSTING_VIEW, BOOKMARK, APPLY -> postingId != null;
			case SHELTER_VIEW -> shelterId != null;
			case DONATION_LINK_CLICK -> postingId != null || shelterId != null;
		};
	}
}

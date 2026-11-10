package com.jjonggeut.domain.recommendation.dto;

import java.time.LocalDateTime;

/** clickedAt 은 최초 클릭 시각, clickCount 는 누적 클릭 수다. */
public record ClickResponse(
		Long recommendationBatchId,
		Long recommendationItemId,
		Long postingId,
		int clickCount,
		LocalDateTime clickedAt
) {
}

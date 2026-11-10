package com.jjonggeut.domain.recommendation.dto;

import java.util.List;

/**
 * recordedItemIds: 이번 요청으로 새로 노출 기록된 항목,
 * alreadyRecordedItemIds: 이미 노출로 기록되어 있던 항목.
 */
public record ImpressionResponse(
		Long recommendationBatchId,
		List<Long> recordedItemIds,
		List<Long> alreadyRecordedItemIds
) {
}

package com.jjonggeut.domain.recommendation.dto;

import com.jjonggeut.domain.recommendation.entity.RecommendationSource;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import java.time.LocalDateTime;
import java.util.List;

/** AI 맞춤 활동 추천 응답. 추천할 공고가 없으면 items 가 비고 recommendationBatchId 는 null 이다. */
public record RecommendationResponse(
		Long recommendationBatchId,
		RecommendationSource source,
		LocalDateTime generatedAt,
		List<Item> items
) {

	public record Item(
			Long recommendationItemId,
			int rank,
			PostingCard posting,
			String reason,
			List<String> reasonTags
	) {
	}

	public record PostingCard(
			Long postingId,
			String title,
			String shelterName,
			Region region,
			Double distanceKm,
			List<ActivityCategory> activityCategories
	) {
	}

	public record Region(String sido, String sigungu) {
	}
}

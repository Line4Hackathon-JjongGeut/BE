package com.jjonggeut.domain.recommendation.service;

import com.jjonggeut.domain.recommendation.entity.PostingRef;
import com.jjonggeut.domain.recommendation.entity.ShelterRef;
import java.util.List;

/** 점수가 계산된 추천 후보. */
public record RecommendationCandidate(
		PostingRef posting,
		ShelterRef shelter,
		Double distanceKm,
		double score,
		List<String> reasonTags,
		String ruleReason
) {
}

package com.jjonggeut.domain.recommendation.entity;

/** 추천 이유를 만든 방식. */
public enum RecommendationSource {
	/** AI 가 추천 이유를 생성 */
	AI,
	/** 규칙 기반 이유 (AI 미사용·동의 없음·AI 실패) */
	RULE
}

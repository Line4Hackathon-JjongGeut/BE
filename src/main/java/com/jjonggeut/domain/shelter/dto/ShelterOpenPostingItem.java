package com.jjonggeut.domain.shelter.dto;

import java.time.LocalDateTime;

/** 시설 상세에 보여 주는 모집 중 공고 요약. */
public record ShelterOpenPostingItem(
		Long postingId,
		String postingType,
		String title,
		LocalDateTime activityStartAt,
		Integer capacity,
		Integer applicantCount) {
}

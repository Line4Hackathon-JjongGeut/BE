package com.jjonggeut.domain.shelter.dto;

import java.time.LocalDateTime;

/** 시설의 보호동물 현황. 등록된 현황이 없으면 개수는 모두 0, updatedAt 은 null 이다. */
public record ShelterAnimalStatsResponse(
		Long shelterId,
		int dogCount,
		int catCount,
		int otherCount,
		int totalCount,
		LocalDateTime updatedAt) {
}

package com.jjonggeut.domain.shelter.dto;

import java.time.LocalDateTime;

/** 관심 시설 저장 결과. */
public record ShelterBookmarkResponse(Long shelterId, boolean bookmarked, LocalDateTime createdAt) {
}

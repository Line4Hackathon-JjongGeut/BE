package com.jjonggeut.domain.posting.dto;

import java.time.LocalDateTime;

/** 관심 공고 저장 결과. */
public record PostingBookmarkResponse(Long postingId, boolean bookmarked, LocalDateTime createdAt) {
}

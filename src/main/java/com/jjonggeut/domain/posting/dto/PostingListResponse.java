package com.jjonggeut.domain.posting.dto;

import java.util.List;

/** 페이지 없이 목록만 주는 응답. */
public record PostingListResponse(List<PostingSummaryItem> items) {
}

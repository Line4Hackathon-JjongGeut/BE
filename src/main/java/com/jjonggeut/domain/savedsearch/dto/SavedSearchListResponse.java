package com.jjonggeut.domain.savedsearch.dto;

import java.util.List;

/** 기본 조건이 맨 앞, 나머지는 최근 저장한 순서. */
public record SavedSearchListResponse(List<SavedSearchResponse> savedSearches) {
}

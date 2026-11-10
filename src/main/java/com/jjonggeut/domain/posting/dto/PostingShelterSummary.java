package com.jjonggeut.domain.posting.dto;

import com.jjonggeut.domain.activity.dto.RegionResponse;

/** 공고 목록 항목에 붙는 시설 요약. */
public record PostingShelterSummary(Long shelterId, String name, RegionResponse region) {
}

package com.jjonggeut.domain.posting.dto;

import com.jjonggeut.domain.activity.dto.RegionResponse;

/** 공고 목록 항목에 붙는 시설 요약. 좌표가 없으면 latitude/longitude 는 null 이다. */
public record PostingShelterSummary(Long shelterId, String name, RegionResponse region, Double latitude,
		Double longitude) {
}

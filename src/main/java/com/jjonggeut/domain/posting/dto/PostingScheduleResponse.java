package com.jjonggeut.domain.posting.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 봉사 공고의 신청 가능한 일정. applicable 은 공고가 모집 중이고 잔여 정원이 있을 때 true 다. */
public record PostingScheduleResponse(Long postingId, List<Item> items) {

	public record Item(
			Long scheduleId,
			LocalDateTime startAt,
			LocalDateTime endAt,
			int capacity,
			int appliedCount,
			int remainingCount,
			boolean applicable) {
	}
}

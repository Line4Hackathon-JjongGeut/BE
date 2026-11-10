package com.jjonggeut.domain.posting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** 봉사 공고의 신청 가능한 일정(posting_schedule). 조회 전용이며 운영자 기능이 채운다. of() 는 테스트용이다. */
@Entity
@Immutable
@Table(name = "posting_schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostingSchedule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "schedule_id")
	private Long scheduleId;

	@Column(name = "posting_id", nullable = false)
	private Long postingId;

	@Column(name = "start_at", nullable = false)
	private LocalDateTime startAt;

	@Column(name = "end_at", nullable = false)
	private LocalDateTime endAt;

	@Column(name = "capacity", nullable = false)
	private int capacity;

	@Column(name = "applied_count", nullable = false)
	private int appliedCount;

	public static PostingSchedule of(Long postingId, LocalDateTime startAt, LocalDateTime endAt, int capacity,
			int appliedCount) {
		PostingSchedule schedule = new PostingSchedule();
		schedule.postingId = postingId;
		schedule.startAt = startAt;
		schedule.endAt = endAt;
		schedule.capacity = capacity;
		schedule.appliedCount = appliedCount;
		return schedule;
	}

	public int getRemainingCount() {
		return Math.max(0, capacity - appliedCount);
	}
}

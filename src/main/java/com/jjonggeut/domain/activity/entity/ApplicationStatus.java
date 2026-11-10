package com.jjonggeut.domain.activity.entity;

import java.util.List;

/** 봉사 신청 상태. 진행 중(PENDING, CONFIRMED)과 끝난 상태(그 외)로 나뉜다. */
public enum ApplicationStatus {
	/** 신청 후 승인 대기 */
	PENDING,
	/** 시설이 참여를 확정 */
	CONFIRMED,
	/** 활동 완료 */
	COMPLETED,
	/** 신청 취소 */
	CANCELLED,
	/** 시설이 반려 */
	REJECTED,
	/** 확정 후 불참 */
	NO_SHOW;

	/** 아직 끝나지 않은 신청. */
	public static final List<ApplicationStatus> ONGOING = List.of(PENDING, CONFIRMED);

	/** 참여 이력에 보이는 끝난 신청. */
	public static final List<ApplicationStatus> HISTORY = List.of(COMPLETED, CANCELLED, REJECTED, NO_SHOW);

	/** 캘린더에 보이는 신청: 확정된 일정과 완료된 활동. */
	public static final List<ApplicationStatus> CALENDAR = List.of(CONFIRMED, COMPLETED);
}

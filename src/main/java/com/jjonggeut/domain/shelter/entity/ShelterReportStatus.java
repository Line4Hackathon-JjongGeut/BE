package com.jjonggeut.domain.shelter.entity;

import java.util.List;

/** 신고 처리 상태. 사용자가 신고하면 RECEIVED 이고 이후는 관리자 기능에서 바꾼다. */
public enum ShelterReportStatus {
	RECEIVED,
	IN_REVIEW,
	RESOLVED,
	REJECTED;

	/** 아직 처리가 끝나지 않은 상태. 같은 신고를 중복 접수하지 못하게 막을 때 쓴다. */
	public static final List<ShelterReportStatus> OPEN = List.of(RECEIVED, IN_REVIEW);
}

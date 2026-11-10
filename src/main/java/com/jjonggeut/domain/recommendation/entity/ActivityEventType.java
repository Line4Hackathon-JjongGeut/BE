package com.jjonggeut.domain.recommendation.entity;

/** 사용자 행동 이벤트 종류. */
public enum ActivityEventType {
	/** 공고 조회 */
	POSTING_VIEW,
	/** 시설 조회 */
	SHELTER_VIEW,
	/** 공고 북마크 */
	BOOKMARK,
	/** 활동 신청 */
	APPLY,
	/** 후원 링크 클릭 */
	DONATION_LINK_CLICK
}

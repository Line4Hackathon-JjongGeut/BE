package com.jjonggeut.domain.application.entity;

/** 신청 상태를 바꾼 주체. */
public enum StatusActor {
	/** 신청자 본인 */
	USER,
	/** 시설(운영자) */
	FACILITY,
	/** 시스템(자동 처리) */
	SYSTEM
}

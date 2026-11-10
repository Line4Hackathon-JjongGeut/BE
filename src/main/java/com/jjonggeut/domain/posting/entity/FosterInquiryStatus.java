package com.jjonggeut.domain.posting.entity;

/** 임시보호 참여 의사 처리 상태. 전달하면 RECEIVED 이고 이후는 시설 운영 기능에서 바꾼다. */
public enum FosterInquiryStatus {
	RECEIVED,
	CHECKED,
	CLOSED
}

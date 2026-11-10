package com.jjonggeut.domain.consent.entity;

/** 동의 종류. */
public enum ConsentType {
	/** 서비스 이용약관 */
	TERMS,
	/** 개인정보 수집·이용 */
	PRIVACY,
	/** 위치정보 이용 */
	LOCATION,
	/** 제3자 제공 */
	THIRD_PARTY,
	/** AI 맞춤 추천·정리를 위한 정보 처리 */
	AI_PROCESSING
}

package com.jjonggeut.domain.posting.dto;

import jakarta.validation.constraints.Positive;

/** 후원 링크 이동 기록 요청. 추천 목록에서 들어왔다면 그 추천 항목 ID 를 함께 보낸다. */
public record DonationLinkClickRequest(

		@Positive(message = "추천 항목 ID 는 양수여야 합니다.")
		Long recommendationItemId) {
}

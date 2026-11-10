package com.jjonggeut.domain.posting.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 임시보호 참여 의사 요청. 둘 다 선택이며 contactPhone 을 생략하면 내 계정의 전화번호를 쓴다. */
public record FosterInquiryRequest(

		@Size(max = 500, message = "전할 말은 500자 이하로 입력해주세요.")
		String message,

		@Pattern(regexp = "^[0-9-]{9,13}$", message = "연락처는 숫자와 하이픈으로 9~13자 입력해주세요.")
		String contactPhone) {
}

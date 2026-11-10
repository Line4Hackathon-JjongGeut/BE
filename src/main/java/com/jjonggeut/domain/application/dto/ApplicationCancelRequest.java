package com.jjonggeut.domain.application.dto;

import jakarta.validation.constraints.Size;

public record ApplicationCancelRequest(
		@Size(max = 200, message = "취소 사유는 200자 이하로 입력해주세요.")
		String reason) {
}

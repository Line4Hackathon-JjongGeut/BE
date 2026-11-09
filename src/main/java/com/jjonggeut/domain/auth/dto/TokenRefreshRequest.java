package com.jjonggeut.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(

		@NotBlank(message = "리프레시 토큰을 입력해주세요.")
		String refreshToken
) {

	/** 토큰이 로그에 남지 않도록 마스킹한다. */
	@Override
	public String toString() {
		return "TokenRefreshRequest[refreshToken=****]";
	}
}

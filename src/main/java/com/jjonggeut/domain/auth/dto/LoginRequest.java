package com.jjonggeut.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

		@NotBlank(message = "아이디를 입력해주세요.")
		String loginId,

		@NotBlank(message = "비밀번호를 입력해주세요.")
		String password
) {

	/** 비밀번호가 로그에 남지 않도록 마스킹한다. */
	@Override
	public String toString() {
		return "LoginRequest[loginId=" + loginId + ", password=****]";
	}
}

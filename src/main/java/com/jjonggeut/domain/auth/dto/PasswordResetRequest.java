package com.jjonggeut.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(

		@NotBlank(message = "아이디를 입력해주세요.")
		String loginId,

		@NotBlank(message = "이름을 입력해주세요.")
		String name
) {
}

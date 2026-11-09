package com.jjonggeut.domain.auth.dto;

public record PasswordResetResponse(
		String resetToken,
		long expiresIn
) {

	/** 임시 토큰이 로그에 남지 않도록 마스킹한다. */
	@Override
	public String toString() {
		return "PasswordResetResponse[resetToken=****, expiresIn=" + expiresIn + "]";
	}
}

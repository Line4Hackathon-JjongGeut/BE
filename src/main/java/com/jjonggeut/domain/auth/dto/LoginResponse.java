package com.jjonggeut.domain.auth.dto;

public record LoginResponse(
		Long userId,
		String nickname,
		String accessToken,
		String refreshToken,
		String tokenType,
		long accessTokenExpiresIn
) {
}

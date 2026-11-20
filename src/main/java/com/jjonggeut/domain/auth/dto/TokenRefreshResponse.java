package com.jjonggeut.domain.auth.dto;

public record TokenRefreshResponse(
		String accessToken,
		String tokenType,
		long accessTokenExpiresIn
) {
}

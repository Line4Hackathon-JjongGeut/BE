package com.jjonggeut.domain.auth.dto;

import com.jjonggeut.domain.user.entity.User;

public record SignupResponse(
		Long userId,
		String loginId,
		String name,
		String nickname
) {

	public static SignupResponse from(User user) {
		return new SignupResponse(user.getUserId(), user.getLoginId(), user.getName(), user.getNickname());
	}
}

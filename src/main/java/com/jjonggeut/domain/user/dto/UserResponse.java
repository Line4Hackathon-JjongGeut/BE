package com.jjonggeut.domain.user.dto;

import com.jjonggeut.domain.user.entity.Role;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.entity.UserStatus;
import java.time.LocalDateTime;

/** 내 계정 정보. 비밀번호 관련 값은 포함하지 않는다. email, phone 은 없으면 null 이다. */
public record UserResponse(
		Long userId,
		String loginId,
		String name,
		String nickname,
		String email,
		String phone,
		Role role,
		UserStatus status,
		LocalDateTime createdAt
) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getUserId(), user.getLoginId(), user.getName(), user.getNickname(),
				user.getEmail(), user.getPhone(), user.getRole(), user.getStatus(), user.getCreatedAt());
	}
}

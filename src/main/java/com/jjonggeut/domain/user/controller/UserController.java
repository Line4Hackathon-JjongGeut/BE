package com.jjonggeut.domain.user.controller;

import com.jjonggeut.domain.user.dto.UserResponse;
import com.jjonggeut.domain.user.dto.UserUpdateRequest;
import com.jjonggeut.domain.user.service.UserService;
import com.jjonggeut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@Operation(summary = "내 계정 조회", description = "현재 로그인한 사용자의 기본 계정 정보를 조회한다.")
	@GetMapping
	public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal Long userId) {
		return ApiResponse.success(userService.getMe(userId));
	}

	@Operation(summary = "내 계정 수정",
			description = "이름, 닉네임, 연락처, 이메일을 수정한다. 보낸 항목만 바뀌며, 연락처와 이메일은 빈 문자열로 지울 수 있다.")
	@PatchMapping
	public ApiResponse<UserResponse> updateMe(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody UserUpdateRequest request) {
		return ApiResponse.success(userService.updateMe(userId, request));
	}

	@Operation(summary = "회원 탈퇴",
			description = "계정을 탈퇴 상태로 바꾸고 개인정보를 지운다. 로그인 세션, 추천 프로필, 봉사 경험, 추천 기록은 삭제하고 동의 이력은 남긴다.")
	@DeleteMapping
	public ApiResponse<Void> withdraw(@AuthenticationPrincipal Long userId) {
		userService.withdraw(userId);
		return ApiResponse.success();
	}
}

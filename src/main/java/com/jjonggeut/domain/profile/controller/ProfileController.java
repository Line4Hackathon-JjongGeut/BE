package com.jjonggeut.domain.profile.controller;

import com.jjonggeut.domain.profile.dto.ProfileResponse;
import com.jjonggeut.domain.profile.dto.ProfileUpdateRequest;
import com.jjonggeut.domain.profile.service.ProfileService;
import com.jjonggeut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/users/me/profile")
public class ProfileController {

	private final ProfileService profileService;

	public ProfileController(ProfileService profileService) {
		this.profileService = profileService;
	}

	@Operation(summary = "내 추천 프로필 조회",
			description = "지역, 거리, 경험, 관심 동물·활동, 가능 요일·시간을 조회한다. 프로필이 없으면 onboardingCompleted=false 로 응답한다.")
	@GetMapping
	public ApiResponse<ProfileResponse> get(@AuthenticationPrincipal Long userId) {
		return ApiResponse.success(profileService.getMyProfile(userId));
	}

	@Operation(summary = "내 추천 프로필 수정",
			description = "AI 추천과 검색에 사용하는 온보딩 조건을 저장하거나 수정한다. 보낸 항목만 바뀌며, 배열은 []로 비울 수 있고, 프로필이 없으면 새로 만든다.")
	@PatchMapping
	public ApiResponse<ProfileResponse> update(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody ProfileUpdateRequest request) {
		return ApiResponse.success(profileService.updateMyProfile(userId, request));
	}
}

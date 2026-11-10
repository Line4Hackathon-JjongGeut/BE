package com.jjonggeut.domain.profile.controller;

import com.jjonggeut.domain.profile.dto.ProfileResponse;
import com.jjonggeut.domain.profile.service.ProfileService;
import com.jjonggeut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
}

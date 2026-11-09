package com.jjonggeut.domain.volunteerexperience.controller;

import com.jjonggeut.domain.volunteerexperience.dto.ExperienceCreateRequest;
import com.jjonggeut.domain.volunteerexperience.dto.ExperienceListResponse;
import com.jjonggeut.domain.volunteerexperience.dto.ExperienceResponse;
import com.jjonggeut.domain.volunteerexperience.service.VolunteerExperienceService;
import com.jjonggeut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/users/me/volunteer-experiences")
public class VolunteerExperienceController {

	private final VolunteerExperienceService experienceService;

	public VolunteerExperienceController(VolunteerExperienceService experienceService) {
		this.experienceService = experienceService;
	}

	@Operation(summary = "봉사 경험 저장", description = "사용자가 확인·수정한 봉사 경험을 저장한다.")
	@PostMapping
	public ApiResponse<ExperienceResponse> create(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody ExperienceCreateRequest request) {
		return ApiResponse.success(experienceService.create(userId, request));
	}

	@Operation(summary = "내 봉사 경험 목록 조회", description = "저장한 봉사 경험을 최신순으로 조회한다.")
	@GetMapping
	public ApiResponse<ExperienceListResponse> list(@AuthenticationPrincipal Long userId) {
		return ApiResponse.success(experienceService.list(userId));
	}

	@Operation(summary = "봉사 경험 삭제", description = "본인의 봉사 경험을 삭제한다.")
	@DeleteMapping("/{experienceId}")
	public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long experienceId) {
		experienceService.delete(userId, experienceId);
		return ApiResponse.success();
	}
}

package com.jjonggeut.domain.shelter.controller;

import com.jjonggeut.domain.activity.dto.PageResponse;
import com.jjonggeut.domain.shelter.dto.ShelterListItem;
import com.jjonggeut.domain.shelter.service.ShelterQueryService;
import com.jjonggeut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "보호시설")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/shelters")
public class ShelterController {

	private final ShelterQueryService shelterService;

	public ShelterController(ShelterQueryService shelterService) {
		this.shelterService = shelterService;
	}

	@Operation(summary = "보호시설 목록 조회",
			description = "지역, 거리, 인증 상태, 보호 동물 유형 조건으로 시설을 조회한다. 좌표를 보내면 가까운 순, 아니면 이름순이다.")
	@GetMapping
	public ApiResponse<PageResponse<ShelterListItem>> list(@AuthenticationPrincipal Long userId,
			@RequestParam(required = false) String sido, @RequestParam(required = false) String sigungu,
			@RequestParam(required = false) Double latitude, @RequestParam(required = false) Double longitude,
			@RequestParam(required = false) Integer maxDistanceKm,
			@RequestParam(required = false) String verificationStatus,
			@RequestParam(required = false) String animalType, @RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size) {
		return ApiResponse.success(shelterService.list(userId, new ShelterQueryService.Query(sido, sigungu,
				latitude, longitude, maxDistanceKm, verificationStatus, animalType, page, size)));
	}
}

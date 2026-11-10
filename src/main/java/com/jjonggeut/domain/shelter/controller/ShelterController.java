package com.jjonggeut.domain.shelter.controller;

import com.jjonggeut.domain.activity.dto.PageResponse;
import com.jjonggeut.domain.shelter.dto.ShelterAnimalStatsResponse;
import com.jjonggeut.domain.shelter.dto.ShelterDetailResponse;
import com.jjonggeut.domain.shelter.dto.ShelterListItem;
import com.jjonggeut.domain.shelter.dto.ShelterPostingItem;
import com.jjonggeut.domain.shelter.service.ShelterPostingQueryService;
import com.jjonggeut.domain.shelter.service.ShelterQueryService;
import com.jjonggeut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "보호시설")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/shelters")
public class ShelterController {

	private final ShelterQueryService shelterService;
	private final ShelterPostingQueryService postingService;

	public ShelterController(ShelterQueryService shelterService, ShelterPostingQueryService postingService) {
		this.shelterService = shelterService;
		this.postingService = postingService;
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

	@Operation(summary = "보호시설 상세 조회",
			description = "시설 정보, 인증 상태, 연락처(인증 시설만)와 모집 중 공고를 조회한다. 좌표를 보내면 거리를 함께 준다.")
	@GetMapping("/{shelterId}")
	public ApiResponse<ShelterDetailResponse> detail(@AuthenticationPrincipal Long userId,
			@PathVariable Long shelterId, @RequestParam(required = false) Double latitude,
			@RequestParam(required = false) Double longitude) {
		return ApiResponse.success(shelterService.detail(userId, shelterId, latitude, longitude));
	}

	@Operation(summary = "보호동물 현황 조회", description = "시설의 강아지, 고양이, 기타 동물 보호 현황을 조회한다.")
	@GetMapping("/{shelterId}/animal-stats")
	public ApiResponse<ShelterAnimalStatsResponse> animalStats(@PathVariable Long shelterId) {
		return ApiResponse.success(shelterService.animalStats(shelterId));
	}

	@Operation(summary = "시설 모집공고 목록 조회",
			description = "시설의 모집 중인 봉사·후원·임시보호 공고를 활동 시작일 빠른 순으로 조회한다.")
	@GetMapping("/{shelterId}/postings")
	public ApiResponse<PageResponse<ShelterPostingItem>> postings(@AuthenticationPrincipal Long userId,
			@PathVariable Long shelterId, @RequestParam(required = false) String postingType,
			@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
		return ApiResponse.success(postingService.list(userId, shelterId, postingType, page, size));
	}
}

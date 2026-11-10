package com.jjonggeut.domain.recommendation.controller;

import com.jjonggeut.domain.recommendation.dto.ImpressionRequest;
import com.jjonggeut.domain.recommendation.dto.ImpressionResponse;
import com.jjonggeut.domain.recommendation.dto.RecommendationResponse;
import com.jjonggeut.domain.recommendation.service.RecommendationImpressionService;
import com.jjonggeut.domain.recommendation.service.RecommendationService;
import com.jjonggeut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "추천")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

	private final RecommendationService recommendationService;
	private final RecommendationImpressionService impressionService;

	public RecommendationController(RecommendationService recommendationService,
			RecommendationImpressionService impressionService) {
		this.recommendationService = recommendationService;
		this.impressionService = impressionService;
	}

	@Operation(summary = "AI 맞춤 활동 추천",
			description = "사용자 조건, 참여 가능성, 관심 이력과 시설 수요를 반영한 추천 목록과 이유를 반환한다. AI 동의가 없거나 AI 호출에 실패하면 규칙 기반 이유로 대체하고 source=RULE 로 응답한다.")
	@GetMapping
	public ApiResponse<RecommendationResponse> recommend(@AuthenticationPrincipal Long userId,
			@RequestParam(required = false) Integer limit,
			@RequestParam(required = false) Double latitude,
			@RequestParam(required = false) Double longitude) {
		return ApiResponse.success(recommendationService.recommend(userId, limit, latitude, longitude));
	}

	@Operation(summary = "추천 노출 기록",
			description = "추천 항목이 사용자 화면에 노출된 사실을 KPI 분석용으로 기록한다. 항목마다 최초 노출 시각만 기록하며, itemIds 를 생략하면 묶음 전체를 기록한다.")
	@PostMapping("/{batchId}/impressions")
	public ApiResponse<ImpressionResponse> impressions(@AuthenticationPrincipal Long userId,
			@PathVariable Long batchId,
			@Valid @RequestBody(required = false) ImpressionRequest request) {
		return ApiResponse.success(impressionService.record(userId, batchId, request));
	}
}

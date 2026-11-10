package com.jjonggeut.domain.recommendation.controller;

import com.jjonggeut.domain.recommendation.dto.RecommendationResponse;
import com.jjonggeut.domain.recommendation.service.RecommendationService;
import com.jjonggeut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "추천")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

	private final RecommendationService recommendationService;

	public RecommendationController(RecommendationService recommendationService) {
		this.recommendationService = recommendationService;
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
}

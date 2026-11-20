package com.jjonggeut.domain.recommendation.controller;

import com.jjonggeut.domain.recommendation.dto.ActivityEventRequest;
import com.jjonggeut.domain.recommendation.dto.ActivityEventResponse;
import com.jjonggeut.domain.recommendation.service.ActivityEventService;
import com.jjonggeut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "추천")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/activity-events")
public class ActivityEventController {

	private final ActivityEventService activityEventService;

	public ActivityEventController(ActivityEventService activityEventService) {
		this.activityEventService = activityEventService;
	}

	@Operation(summary = "사용자 행동 이벤트 기록",
			description = "공고·시설 조회, 북마크, 신청, 후원 링크 클릭 이벤트를 기록한다. 추천에서 이어진 행동이면 recommendationItemId 를 함께 보내 전환 분석에 연결한다.")
	@PostMapping
	public ApiResponse<ActivityEventResponse> record(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody ActivityEventRequest request) {
		return ApiResponse.success(activityEventService.record(userId, request));
	}
}

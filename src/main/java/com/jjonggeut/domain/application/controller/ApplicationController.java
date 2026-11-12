package com.jjonggeut.domain.application.controller;

import com.jjonggeut.domain.activity.dto.PageResponse;
import com.jjonggeut.domain.application.dto.ApplicationCancelRequest;
import com.jjonggeut.domain.application.dto.ApplicationCancelResponse;
import com.jjonggeut.domain.application.dto.ApplicationDetailResponse;
import com.jjonggeut.domain.application.dto.ApplicationListItem;
import com.jjonggeut.domain.application.dto.ApplicationRequest;
import com.jjonggeut.domain.application.dto.ApplicationResponse;
import com.jjonggeut.domain.application.dto.StatusHistoryResponse;
import com.jjonggeut.domain.application.service.ApplicationQueryService;
import com.jjonggeut.domain.application.service.ApplicationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "봉사신청")
@SecurityRequirement(name = "bearerAuth")
@RestController
public class ApplicationController {

	private final ApplicationService applicationService;
	private final ApplicationQueryService queryService;

	public ApplicationController(ApplicationService applicationService, ApplicationQueryService queryService) {
		this.applicationService = applicationService;
		this.queryService = queryService;
	}

	@Operation(summary = "봉사 신청",
			description = "정원과 중복 신청을 검증하고 봉사 신청을 승인 대기 상태로 접수한다. 필수 질문이 있으면 answers 로 답해야 한다. "
					+ "experienceId 를 지정하면 그 시점 내용을 스냅샷으로 함께 저장한다.")
	@PostMapping("/api/v1/schedules/{scheduleId}/applications")
	public ApiResponse<ApplicationResponse> apply(@AuthenticationPrincipal Long userId,
			@PathVariable Long scheduleId, @Valid @RequestBody(required = false) ApplicationRequest request) {
		return ApiResponse.success(applicationService.apply(userId, scheduleId, request));
	}

	@Operation(summary = "봉사 신청 취소",
			description = "진행 중인 내 신청을 일정 시작 24시간 전까지 취소하고 상태 이력을 기록한다. 취소 사유는 선택이다.")
	@DeleteMapping("/api/v1/applications/{applicationId}")
	public ApiResponse<ApplicationCancelResponse> cancel(@AuthenticationPrincipal Long userId,
			@PathVariable Long applicationId, @Valid @RequestBody(required = false) ApplicationCancelRequest request) {
		return ApiResponse.success(applicationService.cancel(userId, applicationId, request));
	}

	@Operation(summary = "내 봉사 신청 목록 조회",
			description = "상태별로 내 봉사 신청을 최근 신청순으로 조회한다. status 는 상태 하나 또는 ONGOING, HISTORY 묶음이다.")
	@GetMapping("/api/v1/users/me/applications")
	public ApiResponse<PageResponse<ApplicationListItem>> list(@AuthenticationPrincipal Long userId,
			@RequestParam(required = false) String status, @RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size) {
		return ApiResponse.success(queryService.list(userId, status, page, size));
	}

	@Operation(summary = "봉사 신청 상세 조회", description = "내 신청 정보, 상태, 일정, 준비물, 첨부한 경험과 변경 이력을 조회한다.")
	@GetMapping("/api/v1/applications/{applicationId}")
	public ApiResponse<ApplicationDetailResponse> detail(@AuthenticationPrincipal Long userId,
			@PathVariable Long applicationId) {
		return ApiResponse.success(queryService.detail(userId, applicationId));
	}

	@Operation(summary = "신청 상태 이력 조회", description = "신청 접수부터 승인, 반려, 취소, 완료, 불참까지의 상태 변경을 오래된 순으로 조회한다.")
	@GetMapping("/api/v1/applications/{applicationId}/status-history")
	public ApiResponse<StatusHistoryResponse> statusHistory(@AuthenticationPrincipal Long userId,
			@PathVariable Long applicationId) {
		return ApiResponse.success(queryService.statusHistory(userId, applicationId));
	}
}

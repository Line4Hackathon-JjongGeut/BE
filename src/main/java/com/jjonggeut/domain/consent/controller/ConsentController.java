package com.jjonggeut.domain.consent.controller;

import com.jjonggeut.domain.consent.dto.ConsentHistoryResponse;
import com.jjonggeut.domain.consent.dto.ConsentRecordRequest;
import com.jjonggeut.domain.consent.dto.ConsentRecordResponse;
import com.jjonggeut.domain.consent.entity.ConsentType;
import com.jjonggeut.domain.consent.service.ConsentService;
import com.jjonggeut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/users/me/consents")
public class ConsentController {

	private final ConsentService consentService;

	public ConsentController(ConsentService consentService) {
		this.consentService = consentService;
	}

	@Operation(summary = "사용자 동의 기록",
			description = "정책 버전별 동의 또는 철회 사실을 이력으로 저장한다. 호출할 때마다 한 행씩 추가되며, 철회는 agreed=false 로 다시 기록한다.")
	@PostMapping
	public ApiResponse<ConsentRecordResponse> record(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody ConsentRecordRequest request) {
		return ApiResponse.success(consentService.record(userId, request));
	}

	@Operation(summary = "내 동의 이력 조회",
			description = "약관, 개인정보, 위치정보, 제3자 제공, AI 정보 처리 동의 이력을 조회한다. current 는 종류별 현재 상태, history 는 전체 이력(최신순)이다.")
	@GetMapping
	public ApiResponse<ConsentHistoryResponse> list(@AuthenticationPrincipal Long userId,
			@RequestParam(required = false) ConsentType consentType) {
		return ApiResponse.success(consentService.getMyConsents(userId, consentType));
	}
}

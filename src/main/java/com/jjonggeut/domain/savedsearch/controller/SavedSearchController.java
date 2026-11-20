package com.jjonggeut.domain.savedsearch.controller;

import com.jjonggeut.domain.savedsearch.dto.SavedSearchCreateRequest;
import com.jjonggeut.domain.savedsearch.dto.SavedSearchListResponse;
import com.jjonggeut.domain.savedsearch.dto.SavedSearchResponse;
import com.jjonggeut.domain.savedsearch.dto.SavedSearchUpdateRequest;
import com.jjonggeut.domain.savedsearch.service.SavedSearchService;
import com.jjonggeut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/users/me/saved-searches")
public class SavedSearchController {

	private final SavedSearchService savedSearchService;

	public SavedSearchController(SavedSearchService savedSearchService) {
		this.savedSearchService = savedSearchService;
	}

	@Operation(summary = "저장 검색 조건 목록 조회",
			description = "사용자가 저장한 활동 검색 조건을 조회한다. 기본 조건이 맨 앞이고 나머지는 최근 저장한 순서다.")
	@GetMapping
	public ApiResponse<SavedSearchListResponse> list(@AuthenticationPrincipal Long userId) {
		return ApiResponse.success(savedSearchService.list(userId));
	}

	@Operation(summary = "검색 조건 저장",
			description = "지역·거리·요일·시간·활동 유형 필터 조합을 이름을 붙여 저장한다. 최대 10개이며, 기본으로 저장하면 기존 기본 조건은 해제된다.")
	@PostMapping
	public ApiResponse<SavedSearchResponse> create(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody SavedSearchCreateRequest request) {
		return ApiResponse.success(savedSearchService.create(userId, request));
	}

	@Operation(summary = "저장 검색 조건 수정",
			description = "저장된 검색 조건의 이름, 필터 또는 기본 여부를 수정한다. 보낸 항목만 바뀌며 배열은 []로 비울 수 있다.")
	@PatchMapping("/{savedSearchId}")
	public ApiResponse<SavedSearchResponse> update(@AuthenticationPrincipal Long userId,
			@PathVariable Long savedSearchId, @Valid @RequestBody SavedSearchUpdateRequest request) {
		return ApiResponse.success(savedSearchService.update(userId, savedSearchId, request));
	}

	@Operation(summary = "저장 검색 조건 삭제", description = "사용자가 소유한 저장 검색 조건을 삭제한다.")
	@DeleteMapping("/{savedSearchId}")
	public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long savedSearchId) {
		savedSearchService.delete(userId, savedSearchId);
		return ApiResponse.success();
	}
}

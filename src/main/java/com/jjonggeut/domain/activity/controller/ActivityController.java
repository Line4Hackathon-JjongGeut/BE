package com.jjonggeut.domain.activity.controller;

import com.jjonggeut.domain.activity.dto.ActivityCalendarResponse;
import com.jjonggeut.domain.activity.dto.ActivitySummaryResponse;
import com.jjonggeut.domain.activity.dto.BookmarkedPostingItem;
import com.jjonggeut.domain.activity.dto.BookmarkedShelterItem;
import com.jjonggeut.domain.activity.dto.PageResponse;
import com.jjonggeut.domain.activity.dto.ParticipationHistoryItem;
import com.jjonggeut.domain.activity.service.ActivityQueryService;
import com.jjonggeut.domain.activity.service.BookmarkQueryService;
import com.jjonggeut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/users/me")
public class ActivityController {

	private final ActivityQueryService activityService;
	private final BookmarkQueryService bookmarkService;

	public ActivityController(ActivityQueryService activityService, BookmarkQueryService bookmarkService) {
		this.activityService = activityService;
		this.bookmarkService = bookmarkService;
	}

	@Operation(summary = "내 활동 요약 조회",
			description = "관심 공고 수, 진행 중 신청 수, 완료 참여 횟수, 미확인 알림 수를 조회한다.")
	@GetMapping("/activity-summary")
	public ApiResponse<ActivitySummaryResponse> getSummary(@AuthenticationPrincipal Long userId) {
		return ApiResponse.success(activityService.getSummary(userId));
	}

	@Operation(summary = "내 활동 캘린더 조회",
			description = "확정된 참여 일정과 완료된 활동을 월 단위로 조회한다. month(YYYY-MM)를 생략하면 이번 달이다.")
	@GetMapping("/activity-calendar")
	public ApiResponse<ActivityCalendarResponse> getCalendar(@AuthenticationPrincipal Long userId,
			@RequestParam(required = false) String month) {
		return ApiResponse.success(activityService.getCalendar(userId, month));
	}

	@Operation(summary = "내 참여 이력 조회",
			description = "완료, 취소, 반려, 불참 상태의 과거 신청 이력을 활동 시작이 최근인 순서로 조회한다. status 로 거를 수 있다.")
	@GetMapping("/participation-history")
	public ApiResponse<PageResponse<ParticipationHistoryItem>> getHistory(@AuthenticationPrincipal Long userId,
			@RequestParam(required = false) String status, @RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size) {
		return ApiResponse.success(activityService.getHistory(userId, status, page, size));
	}

	@Operation(summary = "내 관심 공고 목록 조회",
			description = "사용자가 저장한 모집공고를 모집 상태와 함께 저장 최신순으로 조회한다. status(OPEN/CLOSED)로 거를 수 있다.")
	@GetMapping("/bookmarked-postings")
	public ApiResponse<PageResponse<BookmarkedPostingItem>> getBookmarkedPostings(
			@AuthenticationPrincipal Long userId, @RequestParam(required = false) String status,
			@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
		return ApiResponse.success(bookmarkService.getBookmarkedPostings(userId, status, page, size));
	}

	@Operation(summary = "내 관심 시설 목록 조회",
			description = "사용자가 저장한 보호시설을 최신 저장순으로 조회한다. 시설마다 모집 중인 공고 수를 함께 준다.")
	@GetMapping("/bookmarked-shelters")
	public ApiResponse<PageResponse<BookmarkedShelterItem>> getBookmarkedShelters(
			@AuthenticationPrincipal Long userId, @RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size) {
		return ApiResponse.success(bookmarkService.getBookmarkedShelters(userId, page, size));
	}
}

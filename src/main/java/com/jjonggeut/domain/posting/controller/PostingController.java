package com.jjonggeut.domain.posting.controller;

import com.jjonggeut.domain.activity.dto.PageResponse;
import com.jjonggeut.domain.posting.dto.DonationLinkClickRequest;
import com.jjonggeut.domain.posting.dto.DonationLinkClickResponse;
import com.jjonggeut.domain.posting.dto.FosterInquiryRequest;
import com.jjonggeut.domain.posting.dto.FosterInquiryResponse;
import com.jjonggeut.domain.posting.dto.PostingBookmarkResponse;
import com.jjonggeut.domain.posting.dto.PostingDetailResponse;
import com.jjonggeut.domain.posting.dto.PostingListResponse;
import com.jjonggeut.domain.posting.dto.PostingQuestionsResponse;
import com.jjonggeut.domain.posting.dto.PostingReportRequest;
import com.jjonggeut.domain.posting.dto.PostingReportResponse;
import com.jjonggeut.domain.posting.dto.PostingScheduleResponse;
import com.jjonggeut.domain.posting.dto.PostingSummaryItem;
import com.jjonggeut.domain.posting.service.FosterInquiryService;
import com.jjonggeut.domain.posting.service.PostingBookmarkService;
import com.jjonggeut.domain.posting.service.PostingDonationService;
import com.jjonggeut.domain.posting.service.PostingQueryService;
import com.jjonggeut.domain.posting.service.PostingReportService;
import com.jjonggeut.domain.posting.service.PostingSearchService;
import com.jjonggeut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "모집공고")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/postings")
public class PostingController {

	private final PostingSearchService searchService;
	private final PostingQueryService queryService;
	private final PostingBookmarkService bookmarkService;
	private final PostingReportService reportService;
	private final PostingDonationService donationService;
	private final FosterInquiryService fosterInquiryService;

	public PostingController(PostingSearchService searchService, PostingQueryService queryService,
			PostingBookmarkService bookmarkService, PostingReportService reportService,
			PostingDonationService donationService, FosterInquiryService fosterInquiryService) {
		this.searchService = searchService;
		this.queryService = queryService;
		this.bookmarkService = bookmarkService;
		this.reportService = reportService;
		this.donationService = donationService;
		this.fosterInquiryService = fosterInquiryService;
	}

	@Operation(summary = "모집공고 검색",
			description = "활동 유형, 위치, 거리, 일정, 경험, 동물, 긴급 여부로 모집 중인 공고를 검색하고 정렬한다.")
	@GetMapping
	public ApiResponse<PageResponse<PostingSummaryItem>> search(@AuthenticationPrincipal Long userId,
			@RequestParam(required = false) String keyword, @RequestParam(required = false) String postingType,
			@RequestParam(required = false) List<String> activityCategories,
			@RequestParam(required = false) String animalType, @RequestParam(required = false) String sido,
			@RequestParam(required = false) String sigungu, @RequestParam(required = false) Double latitude,
			@RequestParam(required = false) Double longitude, @RequestParam(required = false) Integer maxDistanceKm,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			@RequestParam(required = false) Boolean beginnerFriendly,
			@RequestParam(required = false) Boolean urgentOnly, @RequestParam(required = false) String sort,
			@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
		return ApiResponse.success(searchService.search(userId, new PostingSearchService.Query(keyword, postingType,
				activityCategories, animalType, sido, sigungu, latitude, longitude, maxDistanceKm, startDate,
				endDate, beginnerFriendly, urgentOnly, sort, page, size)));
	}

	@Operation(summary = "긴급 모집공고 조회", description = "모집 인력 또는 후원이 긴급한 공개 공고를 우선순위순으로 조회한다.")
	@GetMapping("/urgent")
	public ApiResponse<PostingListResponse> urgent(@AuthenticationPrincipal Long userId,
			@RequestParam(required = false) String postingType, @RequestParam(required = false) Integer limit) {
		return ApiResponse.success(searchService.urgent(userId, postingType, limit));
	}

	@Operation(summary = "모집공고 상세 조회", description = "유형별 상세 정보, 시설 정보, 모집 상태와 참여 조건을 조회한다.")
	@GetMapping("/{postingId}")
	public ApiResponse<PostingDetailResponse> detail(@AuthenticationPrincipal Long userId,
			@PathVariable Long postingId, @RequestParam(required = false) Double latitude,
			@RequestParam(required = false) Double longitude) {
		return ApiResponse.success(queryService.detail(userId, postingId, latitude, longitude));
	}

	@Operation(summary = "봉사 가능 일정 조회", description = "봉사 공고의 신청 가능한 일정과 잔여 정원을 조회한다.")
	@GetMapping("/{postingId}/schedules")
	public ApiResponse<PostingScheduleResponse> schedules(@PathVariable Long postingId) {
		return ApiResponse.success(queryService.schedules(postingId));
	}

	@Operation(summary = "봉사 신청 질문 조회", description = "봉사 신청서에 표시할 필수·선택 질문과 선택지를 조회한다.")
	@GetMapping("/{postingId}/application-questions")
	public ApiResponse<PostingQuestionsResponse> questions(@PathVariable Long postingId) {
		return ApiResponse.success(queryService.questions(postingId));
	}

	@Operation(summary = "관심 공고 저장", description = "공고를 내 관심 목록에 저장한다. 이미 저장했다면 409.")
	@PostMapping("/{postingId}/bookmarks")
	public ApiResponse<PostingBookmarkResponse> saveBookmark(@AuthenticationPrincipal Long userId,
			@PathVariable Long postingId) {
		return ApiResponse.success(bookmarkService.save(userId, postingId));
	}

	@Operation(summary = "관심 공고 해제", description = "공고를 내 관심 목록에서 제거한다. 저장하지 않은 공고면 404.")
	@DeleteMapping("/{postingId}/bookmarks")
	public ApiResponse<Void> removeBookmark(@AuthenticationPrincipal Long userId, @PathVariable Long postingId) {
		bookmarkService.remove(userId, postingId);
		return ApiResponse.success();
	}

	@Operation(summary = "모집공고 신고",
			description = "허위·부적절·오래된 모집 정보를 관리자에게 신고한다. 같은 유형의 처리 중인 신고가 있으면 409.")
	@PostMapping("/{postingId}/reports")
	public ApiResponse<PostingReportResponse> report(@AuthenticationPrincipal Long userId,
			@PathVariable Long postingId, @Valid @RequestBody PostingReportRequest request) {
		return ApiResponse.success(reportService.report(userId, postingId, request));
	}

	@Operation(summary = "후원 링크 이동 기록", description = "공식 외부 후원 링크 클릭 이벤트를 기록한 후 이동할 링크를 반환한다.")
	@PostMapping("/{postingId}/donation-link-clicks")
	public ApiResponse<DonationLinkClickResponse> donationLinkClick(@AuthenticationPrincipal Long userId,
			@PathVariable Long postingId, @Valid @RequestBody(required = false) DonationLinkClickRequest request) {
		return ApiResponse.success(donationService.record(userId, postingId,
				request == null ? null : request.recommendationItemId()));
	}

	@Operation(summary = "임시보호 참여 의사 전달", description = "앱 문의 방식의 임시보호 공고에 참여 의사를 시설에 전달한다.")
	@PostMapping("/{postingId}/foster-inquiries")
	public ApiResponse<FosterInquiryResponse> fosterInquiry(@AuthenticationPrincipal Long userId,
			@PathVariable Long postingId, @Valid @RequestBody(required = false) FosterInquiryRequest request) {
		return ApiResponse.success(fosterInquiryService.inquire(userId, postingId, request));
	}
}

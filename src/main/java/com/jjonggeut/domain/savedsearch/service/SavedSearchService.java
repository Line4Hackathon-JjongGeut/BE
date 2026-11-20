package com.jjonggeut.domain.savedsearch.service;

import com.jjonggeut.domain.savedsearch.dto.SavedSearchCreateRequest;
import com.jjonggeut.domain.savedsearch.dto.SavedSearchListResponse;
import com.jjonggeut.domain.savedsearch.dto.SavedSearchRegionRequest;
import com.jjonggeut.domain.savedsearch.dto.SavedSearchResponse;
import com.jjonggeut.domain.savedsearch.dto.SavedSearchUpdateRequest;
import com.jjonggeut.domain.savedsearch.entity.SavedSearch;
import com.jjonggeut.domain.savedsearch.exception.SavedSearchErrorCode;
import com.jjonggeut.domain.savedsearch.repository.SavedSearchRepository;
import com.jjonggeut.global.exception.BusinessException;
import com.jjonggeut.global.exception.ErrorCode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavedSearchService {

	/** 사용자당 저장할 수 있는 검색 조건 수. */
	static final int MAX_SAVED_SEARCHES = 10;

	private final SavedSearchRepository savedSearchRepository;

	public SavedSearchService(SavedSearchRepository savedSearchRepository) {
		this.savedSearchRepository = savedSearchRepository;
	}

	@Transactional(readOnly = true)
	public SavedSearchListResponse list(Long userId) {
		List<SavedSearchResponse> items = savedSearchRepository
				.findByUserIdOrderByDefaultSearchDescCreatedAtDescSavedSearchIdDesc(userId).stream()
				.map(SavedSearchResponse::from)
				.toList();
		return new SavedSearchListResponse(items);
	}

	@Transactional
	public SavedSearchResponse create(Long userId, SavedSearchCreateRequest request) {
		String name = request.name().trim();
		if (name.isEmpty()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		if (savedSearchRepository.countByUserId(userId) >= MAX_SAVED_SEARCHES) {
			throw new BusinessException(SavedSearchErrorCode.LIMIT_EXCEEDED);
		}
		if (savedSearchRepository.existsByUserIdAndName(userId, name)) {
			throw new BusinessException(SavedSearchErrorCode.NAME_DUPLICATED);
		}

		SavedSearchRegionRequest region = request.region();
		boolean makeDefault = Boolean.TRUE.equals(request.isDefault());
		if (makeDefault) {
			clearDefault(userId);
		}

		SavedSearch saved = savedSearchRepository.save(SavedSearch.create(
				userId,
				name,
				region == null ? null : region.sido().trim(),
				region == null ? null : region.sigungu().trim(),
				region == null ? null : region.latitude(),
				region == null ? null : region.longitude(),
				request.maxDistanceKm(),
				distinct(request.availableDays()),
				distinct(request.availableTimeSlots()),
				distinct(request.activityCategories()),
				makeDefault));
		return SavedSearchResponse.from(saved);
	}

	@Transactional
	public SavedSearchResponse update(Long userId, Long savedSearchId, SavedSearchUpdateRequest request) {
		SavedSearch search = find(userId, savedSearchId);

		// 수정 결과에 지정된 조건이 하나도 남지 않으면, 아무것도 바꾸기 전에 거절한다.
		if (!hasAnyFilterAfter(search, request)) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}

		if (request.name() != null) {
			String name = request.name().trim();
			if (name.isEmpty()) {
				throw new BusinessException(ErrorCode.VALIDATION_FAILED);
			}
			if (!name.equals(search.getName()) && savedSearchRepository.existsByUserIdAndName(userId, name)) {
				throw new BusinessException(SavedSearchErrorCode.NAME_DUPLICATED);
			}
			search.changeName(name);
		}
		if (request.region() != null) {
			SavedSearchRegionRequest region = request.region();
			search.changeRegion(region.sido().trim(), region.sigungu().trim(), region.latitude(),
					region.longitude());
		}
		if (request.maxDistanceKm() != null) {
			search.changeMaxDistanceKm(request.maxDistanceKm());
		}
		if (request.availableDays() != null) {
			search.changeAvailableDays(distinct(request.availableDays()));
		}
		if (request.availableTimeSlots() != null) {
			search.changeAvailableTimeSlots(distinct(request.availableTimeSlots()));
		}
		if (request.activityCategories() != null) {
			search.changeActivityCategories(distinct(request.activityCategories()));
		}
		if (request.isDefault() != null) {
			if (request.isDefault() && !search.isDefaultSearch()) {
				clearDefault(userId);
			}
			search.changeDefaultSearch(request.isDefault());
		}
		return SavedSearchResponse.from(search);
	}

	/** 요청을 적용했을 때 지역·거리·요일·시간대·활동 유형 중 하나라도 남는지 미리 계산한다. */
	private boolean hasAnyFilterAfter(SavedSearch search, SavedSearchUpdateRequest request) {
		boolean region = request.region() != null
				|| search.getRegionSido() != null || search.getRegionSigungu() != null;
		boolean distance = request.maxDistanceKm() != null || search.getMaxDistanceKm() != null;
		boolean days = request.availableDays() != null
				? !distinct(request.availableDays()).isEmpty() : !search.getAvailableDays().isEmpty();
		boolean slots = request.availableTimeSlots() != null
				? !distinct(request.availableTimeSlots()).isEmpty() : !search.getAvailableTimeSlots().isEmpty();
		boolean categories = request.activityCategories() != null
				? !distinct(request.activityCategories()).isEmpty() : !search.getActivityCategories().isEmpty();
		return region || distance || days || slots || categories;
	}

	/** 없거나 다른 사용자의 조건은 모두 404 로 응답한다. 기본 조건을 지워도 다른 조건이 기본이 되지는 않는다. */
	@Transactional
	public void delete(Long userId, Long savedSearchId) {
		savedSearchRepository.delete(find(userId, savedSearchId));
	}

	private SavedSearch find(Long userId, Long savedSearchId) {
		return savedSearchRepository.findBySavedSearchIdAndUserId(savedSearchId, userId)
				.orElseThrow(() -> new BusinessException(SavedSearchErrorCode.SAVED_SEARCH_NOT_FOUND));
	}

	/** 새 기본 조건을 정하기 전에 기존 기본 조건을 해제한다. 기본은 사용자당 최대 1개다. */
	private void clearDefault(Long userId) {
		savedSearchRepository.findByUserIdAndDefaultSearchTrue(userId)
				.forEach(existing -> existing.changeDefaultSearch(false));
		savedSearchRepository.flush();
	}

	/** null 요소와 중복을 제거하고 입력 순서를 유지한다. null 목록은 빈 목록으로 본다. */
	private <T> List<T> distinct(List<T> values) {
		if (values == null) {
			return List.of();
		}
		return List.copyOf(new LinkedHashSet<>(values.stream().filter(Objects::nonNull).toList()));
	}
}

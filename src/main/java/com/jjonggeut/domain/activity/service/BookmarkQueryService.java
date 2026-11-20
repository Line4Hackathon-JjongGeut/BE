package com.jjonggeut.domain.activity.service;

import com.jjonggeut.domain.activity.dto.BookmarkedPostingItem;
import com.jjonggeut.domain.activity.dto.BookmarkedShelterItem;
import com.jjonggeut.domain.activity.dto.PageResponse;
import com.jjonggeut.domain.activity.dto.RegionResponse;
import com.jjonggeut.domain.activity.entity.PostingBookmarkRef;
import com.jjonggeut.domain.activity.entity.ShelterBookmarkRef;
import com.jjonggeut.domain.activity.repository.PostingBookmarkRefRepository;
import com.jjonggeut.domain.activity.repository.ShelterBookmarkRefRepository;
import com.jjonggeut.domain.recommendation.entity.PostingRef;
import com.jjonggeut.domain.recommendation.entity.ShelterRef;
import com.jjonggeut.domain.recommendation.repository.PostingRefRepository;
import com.jjonggeut.global.exception.BusinessException;
import com.jjonggeut.global.exception.ErrorCode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 내 관심 공고, 관심 시설 목록 조회. */
@Service
@Transactional(readOnly = true)
public class BookmarkQueryService {

	private static final String STATUS_OPEN = "OPEN";
	private static final String STATUS_CLOSED = "CLOSED";

	private final PostingBookmarkRefRepository postingBookmarkRepository;
	private final ShelterBookmarkRefRepository shelterBookmarkRepository;
	private final PostingRefRepository postingRepository;

	public BookmarkQueryService(PostingBookmarkRefRepository postingBookmarkRepository,
			ShelterBookmarkRefRepository shelterBookmarkRepository, PostingRefRepository postingRepository) {
		this.postingBookmarkRepository = postingBookmarkRepository;
		this.shelterBookmarkRepository = shelterBookmarkRepository;
		this.postingRepository = postingRepository;
	}

	/** 마감된 공고도 모집 상태와 함께 보여 준다. status(OPEN/CLOSED)로 거를 수 있다. */
	public PageResponse<BookmarkedPostingItem> getBookmarkedPostings(Long userId, String status, Integer page,
			Integer size) {
		if (status != null && !STATUS_OPEN.equals(status) && !STATUS_CLOSED.equals(status)) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		Pageable pageable = PageParams.of(page, size);
		Page<Object[]> result = status == null
				? postingBookmarkRepository.findBookmarked(userId, pageable)
				: postingBookmarkRepository.findBookmarkedByStatus(userId, status, pageable);

		return PageResponse.of(result, row -> {
			PostingBookmarkRef bookmark = (PostingBookmarkRef) row[0];
			PostingRef posting = (PostingRef) row[1];
			ShelterRef shelter = (ShelterRef) row[2];
			return new BookmarkedPostingItem(posting.getPostingId(), posting.getPostingType(), posting.getTitle(),
					posting.getStatus(), shelter.getShelterId(), shelter.getName(), region(shelter),
					posting.getActivityCategories(), posting.getAnimalType(), posting.getActivityStartAt(),
					posting.getCapacity(), posting.getApplicantCount(), bookmark.getCreatedAt());
		});
	}

	/** 저장 최신순. 시설마다 모집 중인 공고 수를 함께 준다. */
	public PageResponse<BookmarkedShelterItem> getBookmarkedShelters(Long userId, Integer page, Integer size) {
		Page<Object[]> result = shelterBookmarkRepository.findBookmarked(userId, PageParams.of(page, size));

		List<Long> shelterIds = result.getContent().stream()
				.map(row -> ((ShelterRef) row[1]).getShelterId())
				.toList();
		Map<Long, Long> openCounts = new HashMap<>();
		if (!shelterIds.isEmpty()) {
			postingRepository.countOpenByShelterIds(shelterIds)
					.forEach(row -> openCounts.put((Long) row[0], (Long) row[1]));
		}

		return PageResponse.of(result, row -> {
			ShelterBookmarkRef bookmark = (ShelterBookmarkRef) row[0];
			ShelterRef shelter = (ShelterRef) row[1];
			return new BookmarkedShelterItem(shelter.getShelterId(), shelter.getName(), region(shelter),
					openCounts.getOrDefault(shelter.getShelterId(), 0L), bookmark.getCreatedAt());
		});
	}

	private RegionResponse region(ShelterRef shelter) {
		if (shelter.getRegionSido() == null && shelter.getRegionSigungu() == null) {
			return null;
		}
		return new RegionResponse(shelter.getRegionSido(), shelter.getRegionSigungu());
	}
}

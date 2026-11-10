package com.jjonggeut.domain.shelter.service;

import com.jjonggeut.domain.activity.dto.PageResponse;
import com.jjonggeut.domain.activity.repository.PostingBookmarkRefRepository;
import com.jjonggeut.domain.activity.service.PageParams;
import com.jjonggeut.domain.recommendation.entity.PostingRef;
import com.jjonggeut.domain.recommendation.repository.PostingRefRepository;
import com.jjonggeut.domain.recommendation.repository.ShelterRefRepository;
import com.jjonggeut.domain.shelter.dto.ShelterPostingItem;
import com.jjonggeut.domain.shelter.exception.ShelterErrorCode;
import com.jjonggeut.global.exception.BusinessException;
import com.jjonggeut.global.exception.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 시설의 모집 중 공고 목록. */
@Service
@Transactional(readOnly = true)
public class ShelterPostingQueryService {

	/** 공고 종류: 봉사, 후원, 임시보호. 모집공고 API 에서 값이 확정되면 그에 맞춘다. */
	private static final Set<String> POSTING_TYPES = Set.of("VOLUNTEER", "SPONSOR", "FOSTER");

	private final ShelterRefRepository shelterRepository;
	private final PostingRefRepository postingRepository;
	private final PostingBookmarkRefRepository postingBookmarkRepository;

	public ShelterPostingQueryService(ShelterRefRepository shelterRepository,
			PostingRefRepository postingRepository, PostingBookmarkRefRepository postingBookmarkRepository) {
		this.shelterRepository = shelterRepository;
		this.postingRepository = postingRepository;
		this.postingBookmarkRepository = postingBookmarkRepository;
	}

	public PageResponse<ShelterPostingItem> list(Long userId, Long shelterId, String postingType, Integer page,
			Integer size) {
		if (shelterId == null || shelterId < 1 || postingType != null && !POSTING_TYPES.contains(postingType)) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		Pageable pageable = PageParams.of(page, size);
		if (!shelterRepository.existsById(shelterId)) {
			throw new BusinessException(ShelterErrorCode.SHELTER_NOT_FOUND);
		}

		Page<PostingRef> postings = postingType == null
				? postingRepository.findOpenPageByShelterId(shelterId, pageable)
				: postingRepository.findOpenPageByShelterIdAndType(shelterId, postingType, pageable);

		List<Long> ids = postings.getContent().stream().map(PostingRef::getPostingId).toList();
		Set<Long> bookmarked = new HashSet<>();
		if (!ids.isEmpty()) {
			bookmarked.addAll(postingBookmarkRepository.findBookmarkedPostingIds(userId, ids));
		}
		return PageResponse.of(postings, p -> new ShelterPostingItem(p.getPostingId(), p.getPostingType(),
				p.getTitle(), p.getActivityCategories(), p.getAnimalType(), p.getActivityStartAt(),
				p.getCapacity(), p.getApplicantCount(), remaining(p), bookmarked.contains(p.getPostingId())));
	}

	private Integer remaining(PostingRef posting) {
		if (posting.getCapacity() == null) {
			return null;
		}
		int applicants = posting.getApplicantCount() == null ? 0 : posting.getApplicantCount();
		return Math.max(0, posting.getCapacity() - applicants);
	}
}

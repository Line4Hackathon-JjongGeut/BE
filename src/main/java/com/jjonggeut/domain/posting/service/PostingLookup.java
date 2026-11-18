package com.jjonggeut.domain.posting.service;

import com.jjonggeut.domain.posting.exception.PostingErrorCode;
import com.jjonggeut.domain.recommendation.entity.PostingRef;
import com.jjonggeut.domain.recommendation.repository.PostingRefRepository;
import com.jjonggeut.global.exception.BusinessException;
import com.jjonggeut.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

/** 공고 ID 를 확인하고 공개된 공고만 돌려준다. 비공개 공고는 없는 공고와 같게 404 로 응답한다. */
@Component
public class PostingLookup {

	private final PostingRefRepository postingRepository;

	public PostingLookup(PostingRefRepository postingRepository) {
		this.postingRepository = postingRepository;
	}

	public PostingRef requireVisible(Long postingId) {
		if (postingId == null || postingId < 1) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		return postingRepository.findById(postingId)
				.filter(PostingRef::isPublicPosting)
				.orElseThrow(() -> new BusinessException(PostingErrorCode.POSTING_NOT_FOUND));
	}

	/** 남은 자리. 모집 인원이 없으면 null, 신청이 넘쳐도 0 이다. */
	public static Integer remaining(Integer capacity, Integer applicants) {
		if (capacity == null) {
			return null;
		}
		return Math.max(0, capacity - (applicants == null ? 0 : applicants));
	}

	/** 후원 물품 진행률(%). 목표 수량이 없거나 0이면 null, 100을 넘어도 100으로 본다. */
	public static Integer donationProgressRate(Integer target, Integer received) {
		if (target == null || target <= 0) {
			return null;
		}
		int actual = received == null ? 0 : received;
		return Math.min(100, Math.round(actual * 100f / target));
	}
}

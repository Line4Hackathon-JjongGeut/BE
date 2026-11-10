package com.jjonggeut.domain.recommendation.service;

import com.jjonggeut.domain.recommendation.dto.ActivityEventRequest;
import com.jjonggeut.domain.recommendation.dto.ActivityEventResponse;
import com.jjonggeut.domain.recommendation.entity.RecommendationItem;
import com.jjonggeut.domain.recommendation.entity.UserActivityEvent;
import com.jjonggeut.domain.recommendation.exception.RecommendationErrorCode;
import com.jjonggeut.domain.recommendation.repository.RecommendationBatchRepository;
import com.jjonggeut.domain.recommendation.repository.RecommendationItemRepository;
import com.jjonggeut.domain.recommendation.repository.UserActivityEventRepository;
import com.jjonggeut.global.exception.BusinessException;
import com.jjonggeut.global.exception.ErrorCode;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공고·시설 조회, 북마크, 신청, 후원 링크 클릭 같은 사용자 행동을 기록한다. */
@Service
public class ActivityEventService {

	/** 기기 시계 오차를 감안해 이 정도까지의 미래 시각은 허용한다. */
	private static final long FUTURE_TOLERANCE_MINUTES = 5;

	private final UserActivityEventRepository eventRepository;
	private final RecommendationItemRepository itemRepository;
	private final RecommendationBatchRepository batchRepository;

	public ActivityEventService(UserActivityEventRepository eventRepository,
			RecommendationItemRepository itemRepository, RecommendationBatchRepository batchRepository) {
		this.eventRepository = eventRepository;
		this.itemRepository = itemRepository;
		this.batchRepository = batchRepository;
	}

	@Transactional
	public ActivityEventResponse record(Long userId, ActivityEventRequest request) {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime occurredAt = request.occurredAt() != null ? request.occurredAt() : now;
		if (occurredAt.isAfter(now.plusMinutes(FUTURE_TOLERANCE_MINUTES))) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}

		if (request.recommendationItemId() != null) {
			verifyRecommendationItem(userId, request);
		}

		UserActivityEvent saved = eventRepository.save(UserActivityEvent.create(userId, request.eventType(),
				request.postingId(), request.shelterId(), request.recommendationItemId(), occurredAt));
		return ActivityEventResponse.from(saved);
	}

	/** 내 추천 항목이어야 하고, 공고 ID 를 함께 보냈다면 그 항목의 공고와 같아야 한다. */
	private void verifyRecommendationItem(Long userId, ActivityEventRequest request) {
		RecommendationItem item = itemRepository.findById(request.recommendationItemId())
				.orElseThrow(() -> new BusinessException(RecommendationErrorCode.RECOMMENDATION_NOT_FOUND));
		batchRepository.findByRecommendationBatchIdAndUserId(item.getRecommendationBatchId(), userId)
				.orElseThrow(() -> new BusinessException(RecommendationErrorCode.RECOMMENDATION_NOT_FOUND));

		if (request.postingId() != null && !request.postingId().equals(item.getPostingId())) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
	}
}

package com.jjonggeut.domain.recommendation.service;

import com.jjonggeut.domain.recommendation.dto.ClickResponse;
import com.jjonggeut.domain.recommendation.entity.RecommendationItem;
import com.jjonggeut.domain.recommendation.exception.RecommendationErrorCode;
import com.jjonggeut.domain.recommendation.repository.RecommendationBatchRepository;
import com.jjonggeut.domain.recommendation.repository.RecommendationItemRepository;
import com.jjonggeut.global.exception.BusinessException;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 추천 항목 클릭을 기록한다. 이 항목 ID 를 이후 신청·행동 이벤트와 함께 보내면 전환 분석에 연결할 수 있다. */
@Service
public class RecommendationClickService {

	private final RecommendationBatchRepository batchRepository;
	private final RecommendationItemRepository itemRepository;

	public RecommendationClickService(RecommendationBatchRepository batchRepository,
			RecommendationItemRepository itemRepository) {
		this.batchRepository = batchRepository;
		this.itemRepository = itemRepository;
	}

	/** 묶음·항목이 없거나, 타인의 것이거나, 항목이 묶음에 속하지 않는 경우 모두 404 로 응답한다. */
	@Transactional
	public ClickResponse record(Long userId, Long batchId, Long itemId) {
		batchRepository.findByRecommendationBatchIdAndUserId(batchId, userId)
				.orElseThrow(() -> new BusinessException(RecommendationErrorCode.RECOMMENDATION_NOT_FOUND));
		RecommendationItem item = itemRepository
				.findByRecommendationItemIdAndRecommendationBatchId(itemId, batchId)
				.orElseThrow(() -> new BusinessException(RecommendationErrorCode.RECOMMENDATION_NOT_FOUND));

		item.recordClick(LocalDateTime.now());
		return new ClickResponse(batchId, item.getRecommendationItemId(), item.getPostingId(),
				item.getClickCount(), item.getClickedAt());
	}
}

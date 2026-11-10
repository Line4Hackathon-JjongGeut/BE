package com.jjonggeut.domain.recommendation.service;

import com.jjonggeut.domain.recommendation.dto.ImpressionRequest;
import com.jjonggeut.domain.recommendation.dto.ImpressionResponse;
import com.jjonggeut.domain.recommendation.entity.RecommendationItem;
import com.jjonggeut.domain.recommendation.exception.RecommendationErrorCode;
import com.jjonggeut.domain.recommendation.repository.RecommendationBatchRepository;
import com.jjonggeut.domain.recommendation.repository.RecommendationItemRepository;
import com.jjonggeut.global.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 추천 항목이 사용자 화면에 노출된 사실을 기록한다. 항목마다 최초 노출 시각만 남긴다. */
@Service
public class RecommendationImpressionService {

	private final RecommendationBatchRepository batchRepository;
	private final RecommendationItemRepository itemRepository;

	public RecommendationImpressionService(RecommendationBatchRepository batchRepository,
			RecommendationItemRepository itemRepository) {
		this.batchRepository = batchRepository;
		this.itemRepository = itemRepository;
	}

	@Transactional
	public ImpressionResponse record(Long userId, Long batchId, ImpressionRequest request) {
		// 없거나 타인의 묶음은 모두 404 로 응답한다.
		batchRepository.findByRecommendationBatchIdAndUserId(batchId, userId)
				.orElseThrow(() -> new BusinessException(RecommendationErrorCode.RECOMMENDATION_NOT_FOUND));

		List<RecommendationItem> batchItems = itemRepository.findByRecommendationBatchIdOrderByRankNoAsc(batchId);
		List<RecommendationItem> targets = selectTargets(batchItems, request);

		LocalDateTime now = LocalDateTime.now();
		List<Long> recorded = new ArrayList<>();
		List<Long> already = new ArrayList<>();
		for (RecommendationItem item : targets) {
			if (item.markImpressed(now)) {
				recorded.add(item.getRecommendationItemId());
			} else {
				already.add(item.getRecommendationItemId());
			}
		}
		return new ImpressionResponse(batchId, recorded, already);
	}

	/** itemIds 가 없으면 묶음 전체(순위순), 있으면 요청한 순서대로. 묶음에 없는 항목이 있으면 아무것도 기록하지 않는다. */
	private List<RecommendationItem> selectTargets(List<RecommendationItem> batchItems, ImpressionRequest request) {
		if (request == null || request.itemIds() == null) {
			return batchItems;
		}
		Map<Long, RecommendationItem> byId = batchItems.stream()
				.collect(Collectors.toMap(RecommendationItem::getRecommendationItemId, Function.identity()));
		List<RecommendationItem> targets = new ArrayList<>();
		for (Long itemId : request.itemIds()) {
			RecommendationItem item = byId.get(itemId);
			if (item == null) {
				throw new BusinessException(RecommendationErrorCode.ITEM_NOT_IN_BATCH);
			}
			targets.add(item);
		}
		return targets;
	}
}

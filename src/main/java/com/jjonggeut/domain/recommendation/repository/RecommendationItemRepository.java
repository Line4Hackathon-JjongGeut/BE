package com.jjonggeut.domain.recommendation.repository;

import com.jjonggeut.domain.recommendation.entity.RecommendationItem;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface RecommendationItemRepository extends JpaRepository<RecommendationItem, Long> {

	List<RecommendationItem> findByRecommendationBatchIdOrderByRankNoAsc(Long recommendationBatchId);

	/** 동시에 클릭해도 클릭 수가 덮어써지지 않도록 행을 잠그고 조회한다. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<RecommendationItem> findByRecommendationItemIdAndRecommendationBatchId(Long recommendationItemId,
			Long recommendationBatchId);
}

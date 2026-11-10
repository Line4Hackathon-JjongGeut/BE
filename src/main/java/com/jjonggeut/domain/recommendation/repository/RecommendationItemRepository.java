package com.jjonggeut.domain.recommendation.repository;

import com.jjonggeut.domain.recommendation.entity.RecommendationItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationItemRepository extends JpaRepository<RecommendationItem, Long> {

	List<RecommendationItem> findByRecommendationBatchIdOrderByRankNoAsc(Long recommendationBatchId);
}

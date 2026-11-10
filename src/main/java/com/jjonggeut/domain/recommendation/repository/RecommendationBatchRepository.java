package com.jjonggeut.domain.recommendation.repository;

import com.jjonggeut.domain.recommendation.entity.RecommendationBatch;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationBatchRepository extends JpaRepository<RecommendationBatch, Long> {

	Optional<RecommendationBatch> findByRecommendationBatchIdAndUserId(Long recommendationBatchId, Long userId);
}

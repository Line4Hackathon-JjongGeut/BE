package com.jjonggeut.domain.recommendation.repository;

import com.jjonggeut.domain.recommendation.entity.RecommendationBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationBatchRepository extends JpaRepository<RecommendationBatch, Long> {
}

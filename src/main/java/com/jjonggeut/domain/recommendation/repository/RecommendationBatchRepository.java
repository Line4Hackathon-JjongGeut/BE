package com.jjonggeut.domain.recommendation.repository;

import com.jjonggeut.domain.recommendation.entity.RecommendationBatch;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationBatchRepository extends JpaRepository<RecommendationBatch, Long> {

	Optional<RecommendationBatch> findByRecommendationBatchIdAndUserId(Long recommendationBatchId, Long userId);

	/** 회원 탈퇴 시 추천 묶음을 지운다. 항목을 먼저 지운 뒤 호출한다. */
	void deleteByUserId(Long userId);
}

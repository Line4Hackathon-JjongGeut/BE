package com.jjonggeut.domain.recommendation.repository;

import com.jjonggeut.domain.recommendation.entity.RecommendationItem;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecommendationItemRepository extends JpaRepository<RecommendationItem, Long> {

	List<RecommendationItem> findByRecommendationBatchIdOrderByRankNoAsc(Long recommendationBatchId);

	/** 회원 탈퇴 시 그 사용자의 추천 항목을 모두 지운다. */
	@Modifying(flushAutomatically = true)
	@Query("delete from RecommendationItem i where i.recommendationBatchId in "
			+ "(select b.recommendationBatchId from RecommendationBatch b where b.userId = :userId)")
	void deleteByUserId(@Param("userId") Long userId);

	/** 동시에 클릭해도 클릭 수가 덮어써지지 않도록 행을 잠그고 조회한다. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<RecommendationItem> findByRecommendationItemIdAndRecommendationBatchId(Long recommendationItemId,
			Long recommendationBatchId);
}

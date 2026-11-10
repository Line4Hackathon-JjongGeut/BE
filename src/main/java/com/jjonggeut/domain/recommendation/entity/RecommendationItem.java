package com.jjonggeut.domain.recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/** 추천 묶음 안의 추천 한 건. 노출·클릭 기록과 추천 평가의 기준이 된다. */
@Entity
@Table(name = "recommendation_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "recommendation_item_id")
	private Long recommendationItemId;

	@Column(name = "recommendation_batch_id", nullable = false)
	private Long recommendationBatchId;

	@Column(name = "posting_id", nullable = false)
	private Long postingId;

	@Column(name = "rank_no", nullable = false)
	private int rankNo;

	@Column(name = "score")
	private Double score;

	@Column(name = "reason", nullable = false, length = 500)
	private String reason;

	@Convert(converter = StringListConverter.class)
	@Column(name = "reason_tags", nullable = false, length = 500)
	private List<String> reasonTags;

	/** 사용자 화면에 노출된 시각. 노출 기록 API 가 채운다. */
	@Column(name = "impressed_at")
	private LocalDateTime impressedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public static RecommendationItem create(Long batchId, Long postingId, int rankNo, double score, String reason,
			List<String> reasonTags) {
		RecommendationItem item = new RecommendationItem();
		item.recommendationBatchId = batchId;
		item.postingId = postingId;
		item.rankNo = rankNo;
		item.score = score;
		item.reason = reason;
		item.reasonTags = reasonTags;
		return item;
	}
}

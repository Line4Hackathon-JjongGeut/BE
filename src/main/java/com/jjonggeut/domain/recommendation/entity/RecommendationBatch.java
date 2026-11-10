package com.jjonggeut.domain.recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/** 한 번의 추천 요청으로 만들어진 추천 묶음. */
@Entity
@Table(name = "recommendation_batch")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationBatch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "recommendation_batch_id")
	private Long recommendationBatchId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "source", nullable = false, length = 20)
	private RecommendationSource source;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public static RecommendationBatch create(Long userId, RecommendationSource source) {
		RecommendationBatch batch = new RecommendationBatch();
		batch.userId = userId;
		batch.source = source;
		return batch;
	}
}

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

/** 추천 개선과 전환 분석에 쓰는 사용자 행동 이벤트. 쌓기만 하고 수정하지 않는다. */
@Entity
@Table(name = "user_activity_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserActivityEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "activity_event_id")
	private Long activityEventId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 30)
	private ActivityEventType eventType;

	@Column(name = "posting_id")
	private Long postingId;

	@Column(name = "shelter_id")
	private Long shelterId;

	/** 이 행동이 어느 추천에서 이어졌는지 연결한다. 없으면 추천과 무관한 행동이다. */
	@Column(name = "recommendation_item_id")
	private Long recommendationItemId;

	@Column(name = "occurred_at", nullable = false)
	private LocalDateTime occurredAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public static UserActivityEvent create(Long userId, ActivityEventType eventType, Long postingId, Long shelterId,
			Long recommendationItemId, LocalDateTime occurredAt) {
		UserActivityEvent event = new UserActivityEvent();
		event.userId = userId;
		event.eventType = eventType;
		event.postingId = postingId;
		event.shelterId = shelterId;
		event.recommendationItemId = recommendationItemId;
		event.occurredAt = occurredAt;
		return event;
	}
}

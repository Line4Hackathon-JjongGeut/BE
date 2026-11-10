package com.jjonggeut.domain.activity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** 알림(notification)의 읽기 전용 매핑. 미확인 알림 수(read_at 이 비어 있는 것)를 세는 데만 쓴다. */
@Entity
@Immutable
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationRef {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "notification_id")
	private Long notificationId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "read_at")
	private LocalDateTime readAt;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public static NotificationRef of(Long userId, LocalDateTime readAt, LocalDateTime createdAt) {
		NotificationRef notification = new NotificationRef();
		notification.userId = userId;
		notification.readAt = readAt;
		notification.createdAt = createdAt;
		return notification;
	}
}

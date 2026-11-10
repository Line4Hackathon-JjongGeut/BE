package com.jjonggeut.domain.application.entity;

import com.jjonggeut.domain.activity.entity.ApplicationStatus;
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

/** 봉사 신청의 상태 변경 이력(application_status_history). 한 번 쌓으면 고치지 않는다. */
@Entity
@Table(name = "application_status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationStatusHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "history_id")
	private Long historyId;

	@Column(name = "application_id", nullable = false)
	private Long applicationId;

	/** 최초 접수는 null. */
	@Enumerated(EnumType.STRING)
	@Column(name = "from_status", length = 20)
	private ApplicationStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", nullable = false, length = 20)
	private ApplicationStatus toStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "actor", nullable = false, length = 20)
	private StatusActor actor;

	@Column(name = "reason", length = 255)
	private String reason;

	@Column(name = "changed_at", nullable = false)
	private LocalDateTime changedAt;

	public static ApplicationStatusHistory of(Long applicationId, ApplicationStatus fromStatus,
			ApplicationStatus toStatus, StatusActor actor, String reason, LocalDateTime changedAt) {
		ApplicationStatusHistory history = new ApplicationStatusHistory();
		history.applicationId = applicationId;
		history.fromStatus = fromStatus;
		history.toStatus = toStatus;
		history.actor = actor;
		history.reason = reason;
		history.changedAt = changedAt;
		return history;
	}
}

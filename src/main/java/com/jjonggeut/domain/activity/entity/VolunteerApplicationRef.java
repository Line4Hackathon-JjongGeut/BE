package com.jjonggeut.domain.activity.entity;

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
import org.hibernate.annotations.Immutable;

/** 봉사 신청(volunteer_application)의 읽기 전용 매핑. 신청·취소·승인은 봉사신청 API 의 몫이다. */
@Entity
@Immutable
@Table(name = "volunteer_application")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VolunteerApplicationRef {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "application_id")
	private Long applicationId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "posting_id", nullable = false)
	private Long postingId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ApplicationStatus status;

	@Column(name = "applied_at", nullable = false)
	private LocalDateTime appliedAt;

	public static VolunteerApplicationRef of(Long userId, Long postingId, ApplicationStatus status,
			LocalDateTime appliedAt) {
		VolunteerApplicationRef application = new VolunteerApplicationRef();
		application.userId = userId;
		application.postingId = postingId;
		application.status = status;
		application.appliedAt = appliedAt;
		return application;
	}
}

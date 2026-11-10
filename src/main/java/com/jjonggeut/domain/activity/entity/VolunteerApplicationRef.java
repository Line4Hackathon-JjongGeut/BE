package com.jjonggeut.domain.activity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 봉사 신청(volunteer_application). 신청·취소는 봉사신청 API 가 쓰고, 활동 요약·참여 이력·캘린더가 읽는다.
 * scheduleId 가 없는 신청은 일정 도입 전의 예전 데이터다.
 */
@Entity
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

	@Column(name = "schedule_id")
	private Long scheduleId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ApplicationStatus status;

	@Column(name = "applied_at", nullable = false)
	private LocalDateTime appliedAt;

	@Convert(converter = AnswerListConverter.class)
	@Column(name = "answers", length = 4000)
	private List<ApplicationAnswer> answers;

	@Column(name = "message", length = 500)
	private String message;

	/** 테스트와 임시 데이터용. */
	public static VolunteerApplicationRef of(Long userId, Long postingId, ApplicationStatus status,
			LocalDateTime appliedAt) {
		return of(userId, postingId, null, status, appliedAt);
	}

	public static VolunteerApplicationRef of(Long userId, Long postingId, Long scheduleId, ApplicationStatus status,
			LocalDateTime appliedAt) {
		VolunteerApplicationRef application = new VolunteerApplicationRef();
		application.userId = userId;
		application.postingId = postingId;
		application.scheduleId = scheduleId;
		application.status = status;
		application.appliedAt = appliedAt;
		return application;
	}

	/** 새 신청은 항상 승인 대기로 접수한다. */
	public static VolunteerApplicationRef apply(Long userId, Long postingId, Long scheduleId,
			List<ApplicationAnswer> answers, String message, LocalDateTime appliedAt) {
		VolunteerApplicationRef application = of(userId, postingId, scheduleId, ApplicationStatus.PENDING, appliedAt);
		application.answers = answers;
		application.message = message;
		return application;
	}

	public void changeStatus(ApplicationStatus status) {
		this.status = status;
	}

	/** 탈퇴한 사용자의 답변과 남긴 말을 비운다. */
	public void clearPersonalData() {
		this.answers = null;
		this.message = null;
	}

	public List<ApplicationAnswer> getAnswers() {
		return answers == null ? List.of() : answers;
	}
}

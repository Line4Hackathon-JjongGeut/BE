package com.jjonggeut.domain.shelter.entity;

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

/** 사용자가 관리자에게 접수한 보호시설 신고. 신고자가 탈퇴해도 처리를 위해 남긴다. */
@Entity
@Table(name = "shelter_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShelterReport {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "report_id")
	private Long reportId;

	@Column(name = "shelter_id", nullable = false)
	private Long shelterId;

	@Column(name = "reporter_user_id", nullable = false)
	private Long reporterUserId;

	@Enumerated(EnumType.STRING)
	@Column(name = "report_type", nullable = false, length = 20)
	private ShelterReportType reportType;

	@Column(name = "content", length = 1000)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ShelterReportStatus status;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/** 새 신고는 항상 접수 상태로 시작한다. */
	public static ShelterReport receive(Long shelterId, Long reporterUserId, ShelterReportType reportType,
			String content) {
		return of(shelterId, reporterUserId, reportType, content, ShelterReportStatus.RECEIVED);
	}

	public static ShelterReport of(Long shelterId, Long reporterUserId, ShelterReportType reportType, String content,
			ShelterReportStatus status) {
		ShelterReport report = new ShelterReport();
		report.shelterId = shelterId;
		report.reporterUserId = reporterUserId;
		report.reportType = reportType;
		report.content = content;
		report.status = status;
		return report;
	}
}

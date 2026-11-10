package com.jjonggeut.domain.posting.entity;

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

/** 사용자가 관리자에게 접수한 모집공고 신고. 신고자가 탈퇴해도 처리를 위해 남긴다. */
@Entity
@Table(name = "posting_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostingReport {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "report_id")
	private Long reportId;

	@Column(name = "posting_id", nullable = false)
	private Long postingId;

	@Column(name = "reporter_user_id", nullable = false)
	private Long reporterUserId;

	@Enumerated(EnumType.STRING)
	@Column(name = "report_type", nullable = false, length = 20)
	private PostingReportType reportType;

	@Column(name = "content", length = 1000)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private PostingReportStatus status;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public static PostingReport receive(Long postingId, Long reporterUserId, PostingReportType reportType,
			String content) {
		return of(postingId, reporterUserId, reportType, content, PostingReportStatus.RECEIVED);
	}

	public static PostingReport of(Long postingId, Long reporterUserId, PostingReportType reportType, String content,
			PostingReportStatus status) {
		PostingReport report = new PostingReport();
		report.postingId = postingId;
		report.reporterUserId = reporterUserId;
		report.reportType = reportType;
		report.content = content;
		report.status = status;
		return report;
	}
}

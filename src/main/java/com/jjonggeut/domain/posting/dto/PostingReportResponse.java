package com.jjonggeut.domain.posting.dto;

import com.jjonggeut.domain.posting.entity.PostingReport;
import com.jjonggeut.domain.posting.entity.PostingReportStatus;
import com.jjonggeut.domain.posting.entity.PostingReportType;
import java.time.LocalDateTime;

public record PostingReportResponse(
		Long reportId,
		Long postingId,
		PostingReportType reportType,
		PostingReportStatus status,
		LocalDateTime createdAt) {

	public static PostingReportResponse from(PostingReport report) {
		return new PostingReportResponse(report.getReportId(), report.getPostingId(), report.getReportType(),
				report.getStatus(), report.getCreatedAt());
	}
}

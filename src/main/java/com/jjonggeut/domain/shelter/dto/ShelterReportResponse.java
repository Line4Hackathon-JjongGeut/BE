package com.jjonggeut.domain.shelter.dto;

import com.jjonggeut.domain.shelter.entity.ShelterReport;
import com.jjonggeut.domain.shelter.entity.ShelterReportStatus;
import com.jjonggeut.domain.shelter.entity.ShelterReportType;
import java.time.LocalDateTime;

/** 접수된 신고. */
public record ShelterReportResponse(
		Long reportId,
		Long shelterId,
		ShelterReportType reportType,
		ShelterReportStatus status,
		LocalDateTime createdAt) {

	public static ShelterReportResponse from(ShelterReport report) {
		return new ShelterReportResponse(report.getReportId(), report.getShelterId(), report.getReportType(),
				report.getStatus(), report.getCreatedAt());
	}
}

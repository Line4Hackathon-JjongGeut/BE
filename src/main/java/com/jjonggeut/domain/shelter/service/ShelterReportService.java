package com.jjonggeut.domain.shelter.service;

import com.jjonggeut.domain.recommendation.repository.ShelterRefRepository;
import com.jjonggeut.domain.shelter.dto.ShelterReportRequest;
import com.jjonggeut.domain.shelter.dto.ShelterReportResponse;
import com.jjonggeut.domain.shelter.entity.ShelterReport;
import com.jjonggeut.domain.shelter.entity.ShelterReportStatus;
import com.jjonggeut.domain.shelter.exception.ShelterErrorCode;
import com.jjonggeut.domain.shelter.repository.ShelterReportRepository;
import com.jjonggeut.global.exception.BusinessException;
import com.jjonggeut.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 보호시설 신고 접수. */
@Service
@Transactional
public class ShelterReportService {

	private final ShelterRefRepository shelterRepository;
	private final ShelterReportRepository reportRepository;

	public ShelterReportService(ShelterRefRepository shelterRepository, ShelterReportRepository reportRepository) {
		this.shelterRepository = shelterRepository;
		this.reportRepository = reportRepository;
	}

	/** 같은 시설에 같은 유형으로 처리 중인 내 신고가 있으면 409. */
	public ShelterReportResponse report(Long userId, Long shelterId, ShelterReportRequest request) {
		if (shelterId == null || shelterId < 1) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		if (!shelterRepository.existsById(shelterId)) {
			throw new BusinessException(ShelterErrorCode.SHELTER_NOT_FOUND);
		}
		if (reportRepository.existsByReporterUserIdAndShelterIdAndReportTypeAndStatusIn(userId, shelterId,
				request.reportType(), ShelterReportStatus.OPEN)) {
			throw new BusinessException(ShelterErrorCode.REPORT_ALREADY_RECEIVED);
		}
		String content = request.content() == null || request.content().isBlank() ? null : request.content().trim();
		ShelterReport report = reportRepository
				.saveAndFlush(ShelterReport.receive(shelterId, userId, request.reportType(), content));
		return ShelterReportResponse.from(report);
	}
}

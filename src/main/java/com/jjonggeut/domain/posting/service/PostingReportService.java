package com.jjonggeut.domain.posting.service;

import com.jjonggeut.domain.posting.dto.PostingReportRequest;
import com.jjonggeut.domain.posting.dto.PostingReportResponse;
import com.jjonggeut.domain.posting.entity.PostingReport;
import com.jjonggeut.domain.posting.entity.PostingReportStatus;
import com.jjonggeut.domain.posting.exception.PostingErrorCode;
import com.jjonggeut.domain.posting.repository.PostingReportRepository;
import com.jjonggeut.global.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 모집공고 신고 접수. */
@Service
@Transactional
public class PostingReportService {

	private final PostingLookup postingLookup;
	private final PostingReportRepository reportRepository;

	public PostingReportService(PostingLookup postingLookup, PostingReportRepository reportRepository) {
		this.postingLookup = postingLookup;
		this.reportRepository = reportRepository;
	}

	/** 같은 공고에 같은 유형으로 처리 중인 내 신고가 있으면 409. */
	public PostingReportResponse report(Long userId, Long postingId, PostingReportRequest request) {
		postingLookup.requireVisible(postingId);
		if (reportRepository.existsByReporterUserIdAndPostingIdAndReportTypeAndStatusIn(userId, postingId,
				request.reportType(), PostingReportStatus.OPEN)) {
			throw new BusinessException(PostingErrorCode.REPORT_ALREADY_RECEIVED);
		}
		String content = request.content() == null || request.content().isBlank() ? null : request.content().trim();
		PostingReport report = reportRepository
				.saveAndFlush(PostingReport.receive(postingId, userId, request.reportType(), content));
		return PostingReportResponse.from(report);
	}
}

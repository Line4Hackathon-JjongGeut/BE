package com.jjonggeut.domain.posting.repository;

import com.jjonggeut.domain.posting.entity.PostingReport;
import com.jjonggeut.domain.posting.entity.PostingReportStatus;
import com.jjonggeut.domain.posting.entity.PostingReportType;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostingReportRepository extends JpaRepository<PostingReport, Long> {

	boolean existsByReporterUserIdAndPostingIdAndReportTypeAndStatusIn(Long reporterUserId, Long postingId,
			PostingReportType reportType, Collection<PostingReportStatus> statuses);
}

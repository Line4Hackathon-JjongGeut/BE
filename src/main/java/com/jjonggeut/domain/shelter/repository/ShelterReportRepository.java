package com.jjonggeut.domain.shelter.repository;

import com.jjonggeut.domain.shelter.entity.ShelterReport;
import com.jjonggeut.domain.shelter.entity.ShelterReportStatus;
import com.jjonggeut.domain.shelter.entity.ShelterReportType;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShelterReportRepository extends JpaRepository<ShelterReport, Long> {

	boolean existsByReporterUserIdAndShelterIdAndReportTypeAndStatusIn(Long reporterUserId, Long shelterId,
			ShelterReportType reportType, Collection<ShelterReportStatus> statuses);
}

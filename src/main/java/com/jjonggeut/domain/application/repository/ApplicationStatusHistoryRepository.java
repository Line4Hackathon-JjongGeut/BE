package com.jjonggeut.domain.application.repository;

import com.jjonggeut.domain.application.entity.ApplicationStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, Long> {

	/** 오래된 변경부터. */
	List<ApplicationStatusHistory> findByApplicationIdOrderByChangedAtAscHistoryIdAsc(Long applicationId);
}

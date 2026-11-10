package com.jjonggeut.domain.posting.repository;

import com.jjonggeut.domain.posting.entity.PostingSchedule;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostingScheduleRepository extends JpaRepository<PostingSchedule, Long> {

	/** 아직 시작하지 않은 일정을 시작이 빠른 순으로. */
	@Query("""
			select s from PostingSchedule s
			where s.postingId = :postingId and s.startAt > :now
			order by s.startAt, s.scheduleId
			""")
	List<PostingSchedule> findUpcoming(@Param("postingId") Long postingId, @Param("now") LocalDateTime now);
}

package com.jjonggeut.domain.posting.repository;

import com.jjonggeut.domain.posting.entity.PostingSchedule;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

	/** 정원에 여유가 있을 때만 신청 인원을 1 올린다. 동시에 신청해도 정원을 넘지 않으며, 올리지 못하면 0 을 돌려준다. */
	@Modifying(flushAutomatically = true)
	@Query(value = "update posting_schedule set applied_count = applied_count + 1 "
			+ "where schedule_id = :scheduleId and applied_count < capacity", nativeQuery = true)
	int increaseApplied(@Param("scheduleId") Long scheduleId);

	@Modifying(flushAutomatically = true)
	@Query(value = "update posting_schedule set applied_count = applied_count - 1 "
			+ "where schedule_id = :scheduleId and applied_count > 0", nativeQuery = true)
	int decreaseApplied(@Param("scheduleId") Long scheduleId);
}

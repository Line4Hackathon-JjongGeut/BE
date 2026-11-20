package com.jjonggeut.domain.activity.repository;

import com.jjonggeut.domain.activity.entity.ApplicationStatus;
import com.jjonggeut.domain.activity.entity.VolunteerApplicationRef;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VolunteerApplicationRefRepository extends JpaRepository<VolunteerApplicationRef, Long> {

	long countByUserIdAndStatusIn(Long userId, Collection<ApplicationStatus> statuses);

	/** 참여 이력. 행은 [신청, 공고, 시설] 순서이며 활동 시작이 최근인 것부터 정렬한다. */
	@Query(value = """
			select a, p, s from VolunteerApplicationRef a, PostingRef p, ShelterRef s
			where a.userId = :userId and a.status in :statuses
			  and p.postingId = a.postingId and s.shelterId = p.shelterId
			order by p.activityStartAt desc, a.applicationId desc
			""",
			countQuery = """
			select count(a) from VolunteerApplicationRef a, PostingRef p, ShelterRef s
			where a.userId = :userId and a.status in :statuses
			  and p.postingId = a.postingId and s.shelterId = p.shelterId
			""")
	Page<Object[]> findHistory(@Param("userId") Long userId, @Param("statuses") Collection<ApplicationStatus> statuses,
			Pageable pageable);

	/** 활동 시작이 [from, to) 인 신청. 행은 [신청, 공고, 시설] 순서이며 시작이 빠른 것부터 정렬한다. */
	@Query("""
			select a, p, s from VolunteerApplicationRef a, PostingRef p, ShelterRef s
			where a.userId = :userId and a.status in :statuses
			  and p.postingId = a.postingId and s.shelterId = p.shelterId
			  and p.activityStartAt >= :from and p.activityStartAt < :to
			order by p.activityStartAt asc, a.applicationId asc
			""")
	List<Object[]> findCalendar(@Param("userId") Long userId,
			@Param("statuses") Collection<ApplicationStatus> statuses, @Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);

	Optional<VolunteerApplicationRef> findByApplicationIdAndUserId(Long applicationId, Long userId);

	List<VolunteerApplicationRef> findByUserId(Long userId);

	boolean existsByUserIdAndScheduleIdAndStatusIn(Long userId, Long scheduleId,
			Collection<ApplicationStatus> statuses);

	/** 내 신청 목록. 행은 [신청, 공고, 시설, 일정(없을 수 있음)] 순서이며 신청이 최근인 것부터 정렬한다. */
	@Query(value = """
			select a, p, s, sc from VolunteerApplicationRef a
			join PostingRef p on p.postingId = a.postingId
			join ShelterRef s on s.shelterId = p.shelterId
			left join PostingSchedule sc on sc.scheduleId = a.scheduleId
			where a.userId = :userId and a.status in :statuses
			order by a.appliedAt desc, a.applicationId desc
			""",
			countQuery = """
			select count(a) from VolunteerApplicationRef a
			join PostingRef p on p.postingId = a.postingId
			join ShelterRef s on s.shelterId = p.shelterId
			where a.userId = :userId and a.status in :statuses
			""")
	Page<Object[]> findMine(@Param("userId") Long userId, @Param("statuses") Collection<ApplicationStatus> statuses,
			Pageable pageable);
}

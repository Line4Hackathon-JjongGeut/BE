package com.jjonggeut.domain.recommendation.repository;

import com.jjonggeut.domain.recommendation.entity.PostingRef;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostingRefRepository extends JpaRepository<PostingRef, Long> {

	/** 모집 중인 봉사 공고. 활동 일정이 지난 공고는 제외한다. */
	@Query("""
			select p from PostingRef p
			where p.postingType = 'VOLUNTEER' and p.status = 'OPEN'
			  and (p.activityStartAt is null or p.activityStartAt > :now)
			order by p.postingId desc
			""")
	List<PostingRef> findOpenVolunteerPostings(@Param("now") LocalDateTime now, Pageable pageable);
}

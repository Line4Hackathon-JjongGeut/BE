package com.jjonggeut.domain.recommendation.repository;

import com.jjonggeut.domain.recommendation.entity.PostingRef;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
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

	/** 시설의 모집 중 공고. 활동 시작일이 빠른 순이고 일시가 없는 공고는 뒤에 온다. */
	@Query("""
			select p from PostingRef p
			where p.shelterId = :shelterId and p.status = 'OPEN'
			order by case when p.activityStartAt is null then 1 else 0 end, p.activityStartAt, p.postingId
			""")
	List<PostingRef> findOpenByShelterId(@Param("shelterId") Long shelterId, Pageable pageable);

	/** 시설의 모집 중 공고 페이지. 활동 시작일이 빠른 순이고 일시가 없는 공고는 뒤에 온다. */
	@Query(value = """
			select p from PostingRef p
			where p.shelterId = :shelterId and p.status = 'OPEN'
			order by case when p.activityStartAt is null then 1 else 0 end, p.activityStartAt, p.postingId
			""",
			countQuery = "select count(p) from PostingRef p where p.shelterId = :shelterId and p.status = 'OPEN'")
	Page<PostingRef> findOpenPageByShelterId(@Param("shelterId") Long shelterId, Pageable pageable);

	/** {@link #findOpenPageByShelterId} 에서 공고 종류가 일치하는 것만. */
	@Query(value = """
			select p from PostingRef p
			where p.shelterId = :shelterId and p.status = 'OPEN' and p.postingType = :postingType
			order by case when p.activityStartAt is null then 1 else 0 end, p.activityStartAt, p.postingId
			""",
			countQuery = """
			select count(p) from PostingRef p
			where p.shelterId = :shelterId and p.status = 'OPEN' and p.postingType = :postingType
			""")
	Page<PostingRef> findOpenPageByShelterIdAndType(@Param("shelterId") Long shelterId,
			@Param("postingType") String postingType, Pageable pageable);

	/** 시설별 모집 중인 공고 수. 행은 [시설 ID, 개수] 이고 모집 중 공고가 없는 시설은 나오지 않는다. */
	@Query("""
			select p.shelterId, count(p) from PostingRef p
			where p.shelterId in :shelterIds and p.status = 'OPEN'
			group by p.shelterId
			""")
	List<Object[]> countOpenByShelterIds(@Param("shelterIds") Collection<Long> shelterIds);
}

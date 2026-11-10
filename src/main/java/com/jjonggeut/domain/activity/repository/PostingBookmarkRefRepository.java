package com.jjonggeut.domain.activity.repository;

import com.jjonggeut.domain.activity.entity.PostingBookmarkRef;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostingBookmarkRefRepository extends JpaRepository<PostingBookmarkRef, Long> {

	long countByUserId(Long userId);

	/** 회원 탈퇴 시 저장한 공고를 모두 지운다. */
	void deleteByUserId(Long userId);

	/** 저장한 공고 전체. 행은 [북마크, 공고, 시설] 순서이며 저장 최신순이다. */
	@Query(value = """
			select b, p, s from PostingBookmarkRef b, PostingRef p, ShelterRef s
			where b.userId = :userId and p.postingId = b.postingId and s.shelterId = p.shelterId
			order by b.createdAt desc, b.postingBookmarkId desc
			""",
			countQuery = """
			select count(b) from PostingBookmarkRef b, PostingRef p, ShelterRef s
			where b.userId = :userId and p.postingId = b.postingId and s.shelterId = p.shelterId
			""")
	Page<Object[]> findBookmarked(@Param("userId") Long userId, Pageable pageable);

	/** 저장한 공고 중 모집 상태가 일치하는 것. */
	@Query(value = """
			select b, p, s from PostingBookmarkRef b, PostingRef p, ShelterRef s
			where b.userId = :userId and p.postingId = b.postingId and s.shelterId = p.shelterId
			  and p.status = :status
			order by b.createdAt desc, b.postingBookmarkId desc
			""",
			countQuery = """
			select count(b) from PostingBookmarkRef b, PostingRef p, ShelterRef s
			where b.userId = :userId and p.postingId = b.postingId and s.shelterId = p.shelterId
			  and p.status = :status
			""")
	Page<Object[]> findBookmarkedByStatus(@Param("userId") Long userId, @Param("status") String status,
			Pageable pageable);
}

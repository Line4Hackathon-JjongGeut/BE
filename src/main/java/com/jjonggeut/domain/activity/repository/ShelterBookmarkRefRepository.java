package com.jjonggeut.domain.activity.repository;

import com.jjonggeut.domain.activity.entity.ShelterBookmarkRef;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShelterBookmarkRefRepository extends JpaRepository<ShelterBookmarkRef, Long> {

	/** 회원 탈퇴 시 저장한 시설을 모두 지운다. */
	void deleteByUserId(Long userId);

	/** 저장한 시설. 행은 [북마크, 시설] 순서이며 저장 최신순이다. */
	@Query(value = """
			select b, s from ShelterBookmarkRef b, ShelterRef s
			where b.userId = :userId and s.shelterId = b.shelterId
			order by b.createdAt desc, b.shelterBookmarkId desc
			""",
			countQuery = """
			select count(b) from ShelterBookmarkRef b, ShelterRef s
			where b.userId = :userId and s.shelterId = b.shelterId
			""")
	Page<Object[]> findBookmarked(@Param("userId") Long userId, Pageable pageable);
}

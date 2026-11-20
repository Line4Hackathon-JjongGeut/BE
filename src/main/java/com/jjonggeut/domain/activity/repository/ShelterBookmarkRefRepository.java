package com.jjonggeut.domain.activity.repository;

import com.jjonggeut.domain.activity.entity.ShelterBookmarkRef;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShelterBookmarkRefRepository extends JpaRepository<ShelterBookmarkRef, Long> {

	/** 회원 탈퇴 시 저장한 시설을 모두 지운다. */
	void deleteByUserId(Long userId);

	boolean existsByUserIdAndShelterId(Long userId, Long shelterId);

	/** 관심 시설 해제. 지운 행 수를 돌려준다. */
	@Modifying(flushAutomatically = true)
	@Query("delete from ShelterBookmarkRef b where b.userId = :userId and b.shelterId = :shelterId")
	int deleteByUserIdAndShelterId(@Param("userId") Long userId, @Param("shelterId") Long shelterId);

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

	/** 주어진 시설 중 내가 저장한 시설의 ID. */
	@Query("select b.shelterId from ShelterBookmarkRef b where b.userId = :userId and b.shelterId in :shelterIds")
	List<Long> findBookmarkedShelterIds(@Param("userId") Long userId,
			@Param("shelterIds") Collection<Long> shelterIds);
}

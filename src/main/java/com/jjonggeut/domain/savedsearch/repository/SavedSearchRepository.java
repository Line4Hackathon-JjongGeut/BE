package com.jjonggeut.domain.savedsearch.repository;

import com.jjonggeut.domain.savedsearch.entity.SavedSearch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {

	/** 기본 조건을 맨 앞에, 나머지는 최근 저장한 순서로 정렬한다. */
	List<SavedSearch> findByUserIdOrderByDefaultSearchDescCreatedAtDescSavedSearchIdDesc(Long userId);

	Optional<SavedSearch> findBySavedSearchIdAndUserId(Long savedSearchId, Long userId);

	long countByUserId(Long userId);

	boolean existsByUserIdAndName(Long userId, String name);

	List<SavedSearch> findByUserIdAndDefaultSearchTrue(Long userId);

	/** 회원 탈퇴 시 저장한 검색 조건을 모두 지운다. */
	void deleteByUserId(Long userId);
}

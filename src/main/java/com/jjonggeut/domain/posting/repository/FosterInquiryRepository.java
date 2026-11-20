package com.jjonggeut.domain.posting.repository;

import com.jjonggeut.domain.posting.entity.FosterInquiry;
import com.jjonggeut.domain.posting.entity.FosterInquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FosterInquiryRepository extends JpaRepository<FosterInquiry, Long> {

	boolean existsByUserIdAndPostingIdAndStatus(Long userId, Long postingId, FosterInquiryStatus status);

	/** 회원 탈퇴 시 연락처가 담긴 참여 의사를 모두 지운다. */
	void deleteByUserId(Long userId);
}

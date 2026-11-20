package com.jjonggeut.domain.activity.repository;

import com.jjonggeut.domain.activity.entity.NotificationRef;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRefRepository extends JpaRepository<NotificationRef, Long> {

	long countByUserIdAndReadAtIsNull(Long userId);

	/** 회원 탈퇴 시 알림을 모두 지운다. */
	void deleteByUserId(Long userId);
}

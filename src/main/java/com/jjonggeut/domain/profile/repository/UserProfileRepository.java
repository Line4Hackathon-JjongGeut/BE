package com.jjonggeut.domain.profile.repository;

import com.jjonggeut.domain.profile.entity.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

	Optional<UserProfile> findByUserId(Long userId);

	/** 회원 탈퇴 시 프로필을 지운다. */
	void deleteByUserId(Long userId);
}

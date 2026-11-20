package com.jjonggeut.domain.volunteerexperience.repository;

import com.jjonggeut.domain.volunteerexperience.entity.VolunteerExperience;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VolunteerExperienceRepository extends JpaRepository<VolunteerExperience, Long> {

	List<VolunteerExperience> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDescExperienceIdDesc(Long userId);

	Optional<VolunteerExperience> findByExperienceIdAndUserIdAndDeletedAtIsNull(Long experienceId, Long userId);

	/** 회원 탈퇴 시 삭제 표시된 것까지 모두 지운다. */
	void deleteByUserId(Long userId);
}

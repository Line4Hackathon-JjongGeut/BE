package com.jjonggeut.domain.volunteerexperience.repository;

import com.jjonggeut.domain.volunteerexperience.entity.VolunteerExperience;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VolunteerExperienceRepository extends JpaRepository<VolunteerExperience, Long> {

	List<VolunteerExperience> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDescExperienceIdDesc(Long userId);

	Optional<VolunteerExperience> findByExperienceIdAndUserIdAndDeletedAtIsNull(Long experienceId, Long userId);
}

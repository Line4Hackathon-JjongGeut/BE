package com.jjonggeut.domain.application.repository;

import com.jjonggeut.domain.application.entity.ApplicationExperience;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationExperienceRepository extends JpaRepository<ApplicationExperience, Long> {

	Optional<ApplicationExperience> findByApplicationId(Long applicationId);
}

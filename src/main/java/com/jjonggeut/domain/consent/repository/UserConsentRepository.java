package com.jjonggeut.domain.consent.repository;

import com.jjonggeut.domain.consent.entity.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {

	long countByUserId(Long userId);
}

package com.jjonggeut.domain.consent.repository;

import com.jjonggeut.domain.consent.entity.ConsentType;
import com.jjonggeut.domain.consent.entity.UserConsent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {

	long countByUserId(Long userId);

	List<UserConsent> findByUserIdOrderByCreatedAtDescUserConsentIdDesc(Long userId);

	List<UserConsent> findByUserIdAndConsentTypeOrderByCreatedAtDescUserConsentIdDesc(Long userId,
			ConsentType consentType);

	Optional<UserConsent> findFirstByUserIdAndConsentTypeOrderByCreatedAtDescUserConsentIdDesc(Long userId,
			ConsentType consentType);
}

package com.jjonggeut.domain.consent.service;

import com.jjonggeut.domain.consent.dto.ConsentRecordRequest;
import com.jjonggeut.domain.consent.dto.ConsentRecordResponse;
import com.jjonggeut.domain.consent.entity.UserConsent;
import com.jjonggeut.domain.consent.repository.UserConsentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsentService {

	private final UserConsentRepository consentRepository;

	public ConsentService(UserConsentRepository consentRepository) {
		this.consentRepository = consentRepository;
	}

	/** 요청한 동의·철회를 이력으로 한 행씩 추가한다. 기존 이력은 건드리지 않는다. */
	@Transactional
	public ConsentRecordResponse record(Long userId, ConsentRecordRequest request) {
		List<UserConsent> consents = request.consents().stream()
				.map(item -> UserConsent.create(userId, item.consentType(), item.policyVersion().trim(),
						item.agreed()))
				.toList();
		return ConsentRecordResponse.from(consentRepository.saveAll(consents));
	}
}

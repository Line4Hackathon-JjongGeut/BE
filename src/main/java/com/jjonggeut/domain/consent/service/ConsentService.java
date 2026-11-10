package com.jjonggeut.domain.consent.service;

import com.jjonggeut.domain.consent.dto.ConsentHistoryResponse;
import com.jjonggeut.domain.consent.dto.ConsentRecordRequest;
import com.jjonggeut.domain.consent.dto.ConsentRecordResponse;
import com.jjonggeut.domain.consent.entity.ConsentType;
import com.jjonggeut.domain.consent.entity.UserConsent;
import com.jjonggeut.domain.consent.repository.UserConsentRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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

	/**
	 * 내 동의 이력을 최신순으로 조회하고, 종류별로 가장 최근 기록을 현재 상태(current)로 함께 돌려준다.
	 * consentType 을 지정하면 해당 종류만 조회한다. current 는 동의 종류 정의 순서로 정렬한다.
	 */
	@Transactional(readOnly = true)
	public ConsentHistoryResponse getMyConsents(Long userId, ConsentType consentType) {
		List<UserConsent> history = consentType == null
				? consentRepository.findByUserIdOrderByCreatedAtDescUserConsentIdDesc(userId)
				: consentRepository.findByUserIdAndConsentTypeOrderByCreatedAtDescUserConsentIdDesc(userId,
						consentType);

		// 최신순이므로 종류별 첫 항목이 현재 상태다.
		Map<ConsentType, UserConsent> latestByType = new EnumMap<>(ConsentType.class);
		for (UserConsent consent : history) {
			latestByType.putIfAbsent(consent.getConsentType(), consent);
		}

		return new ConsentHistoryResponse(
				latestByType.values().stream().map(ConsentHistoryResponse.CurrentConsent::from).toList(),
				history.stream().map(ConsentRecordResponse.ConsentResponse::from).toList());
	}

	/** 해당 종류의 현재 동의 상태(가장 최근 기록이 동의인지). 기록이 없으면 false. */
	@Transactional(readOnly = true)
	public boolean isAgreed(Long userId, ConsentType consentType) {
		return consentRepository
				.findFirstByUserIdAndConsentTypeOrderByCreatedAtDescUserConsentIdDesc(userId, consentType)
				.map(UserConsent::isAgreed)
				.orElse(false);
	}
}

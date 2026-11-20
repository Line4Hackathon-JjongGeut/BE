package com.jjonggeut.domain.consent.dto;

import com.jjonggeut.domain.consent.entity.ConsentType;
import com.jjonggeut.domain.consent.entity.UserConsent;
import java.time.LocalDateTime;
import java.util.List;

public record ConsentRecordResponse(List<ConsentResponse> consents) {

	public record ConsentResponse(
			Long consentId,
			ConsentType consentType,
			String policyVersion,
			boolean agreed,
			LocalDateTime recordedAt
	) {

		public static ConsentResponse from(UserConsent consent) {
			return new ConsentResponse(
					consent.getUserConsentId(),
					consent.getConsentType(),
					consent.getPolicyVersion(),
					consent.isAgreed(),
					consent.getCreatedAt());
		}
	}

	public static ConsentRecordResponse from(List<UserConsent> consents) {
		return new ConsentRecordResponse(consents.stream().map(ConsentResponse::from).toList());
	}
}

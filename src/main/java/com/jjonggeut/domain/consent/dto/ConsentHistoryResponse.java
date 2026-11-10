package com.jjonggeut.domain.consent.dto;

import com.jjonggeut.domain.consent.dto.ConsentRecordResponse.ConsentResponse;
import com.jjonggeut.domain.consent.entity.ConsentType;
import com.jjonggeut.domain.consent.entity.UserConsent;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 내 동의 이력.
 * current: 종류별로 가장 최근 기록(현재 동의 상태), history: 전체 이력(최신순).
 */
public record ConsentHistoryResponse(List<CurrentConsent> current, List<ConsentResponse> history) {

	public record CurrentConsent(
			ConsentType consentType,
			String policyVersion,
			boolean agreed,
			LocalDateTime recordedAt
	) {

		public static CurrentConsent from(UserConsent consent) {
			return new CurrentConsent(
					consent.getConsentType(),
					consent.getPolicyVersion(),
					consent.isAgreed(),
					consent.getCreatedAt());
		}
	}
}

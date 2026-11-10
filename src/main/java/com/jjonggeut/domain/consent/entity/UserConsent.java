package com.jjonggeut.domain.consent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 정책 버전별 동의·철회 이력. 한 번 저장한 행은 수정하거나 삭제하지 않고 새 행만 추가한다.
 * 현재 동의 상태는 종류별로 가장 최근 행이다.
 */
@Entity
@Table(name = "user_consent")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserConsent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_consent_id")
	private Long userConsentId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "consent_type", nullable = false, length = 30)
	private ConsentType consentType;

	@Column(name = "policy_version", nullable = false, length = 20)
	private String policyVersion;

	@Column(name = "agreed", nullable = false)
	private boolean agreed;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public static UserConsent create(Long userId, ConsentType consentType, String policyVersion, boolean agreed) {
		UserConsent consent = new UserConsent();
		consent.userId = userId;
		consent.consentType = consentType;
		consent.policyVersion = policyVersion;
		consent.agreed = agreed;
		return consent;
	}
}

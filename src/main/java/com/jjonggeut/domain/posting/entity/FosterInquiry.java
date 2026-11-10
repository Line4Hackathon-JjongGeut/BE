package com.jjonggeut.domain.posting.entity;

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

/** 임시보호 공고에 사용자가 전달한 참여 의사. 연락처가 들어 있어 회원 탈퇴 시 삭제한다. */
@Entity
@Table(name = "foster_inquiry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FosterInquiry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "inquiry_id")
	private Long inquiryId;

	@Column(name = "posting_id", nullable = false)
	private Long postingId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "message", length = 500)
	private String message;

	@Column(name = "contact_phone", length = 20)
	private String contactPhone;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private FosterInquiryStatus status;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public static FosterInquiry receive(Long postingId, Long userId, String message, String contactPhone) {
		return of(postingId, userId, message, contactPhone, FosterInquiryStatus.RECEIVED);
	}

	public static FosterInquiry of(Long postingId, Long userId, String message, String contactPhone,
			FosterInquiryStatus status) {
		FosterInquiry inquiry = new FosterInquiry();
		inquiry.postingId = postingId;
		inquiry.userId = userId;
		inquiry.message = message;
		inquiry.contactPhone = contactPhone;
		inquiry.status = status;
		return inquiry;
	}
}

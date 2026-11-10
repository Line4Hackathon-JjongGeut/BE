package com.jjonggeut.domain.posting.dto;

import com.jjonggeut.domain.posting.entity.FosterInquiry;
import com.jjonggeut.domain.posting.entity.FosterInquiryStatus;
import java.time.LocalDateTime;

public record FosterInquiryResponse(Long inquiryId, Long postingId, FosterInquiryStatus status,
		LocalDateTime createdAt) {

	public static FosterInquiryResponse from(FosterInquiry inquiry) {
		return new FosterInquiryResponse(inquiry.getInquiryId(), inquiry.getPostingId(), inquiry.getStatus(),
				inquiry.getCreatedAt());
	}
}

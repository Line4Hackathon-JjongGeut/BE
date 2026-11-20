package com.jjonggeut.domain.posting.dto;

import java.time.LocalDateTime;

/** 이동할 공식 후원 링크와 기록 시각. */
public record DonationLinkClickResponse(Long postingId, String donationUrl, LocalDateTime recordedAt) {
}

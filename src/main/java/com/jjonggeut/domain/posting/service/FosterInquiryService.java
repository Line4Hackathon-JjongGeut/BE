package com.jjonggeut.domain.posting.service;

import com.jjonggeut.domain.posting.dto.FosterInquiryRequest;
import com.jjonggeut.domain.posting.dto.FosterInquiryResponse;
import com.jjonggeut.domain.posting.entity.FosterContactMethod;
import com.jjonggeut.domain.posting.entity.FosterInquiry;
import com.jjonggeut.domain.posting.entity.FosterInquiryStatus;
import com.jjonggeut.domain.posting.entity.PostingType;
import com.jjonggeut.domain.posting.exception.PostingErrorCode;
import com.jjonggeut.domain.posting.repository.FosterInquiryRepository;
import com.jjonggeut.domain.recommendation.entity.PostingRef;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.repository.UserRepository;
import com.jjonggeut.global.exception.BusinessException;
import com.jjonggeut.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 앱 문의 방식의 임시보호 공고에 참여 의사를 전달한다. */
@Service
@Transactional
public class FosterInquiryService {

	private final PostingLookup postingLookup;
	private final FosterInquiryRepository inquiryRepository;
	private final UserRepository userRepository;

	public FosterInquiryService(PostingLookup postingLookup, FosterInquiryRepository inquiryRepository,
			UserRepository userRepository) {
		this.postingLookup = postingLookup;
		this.inquiryRepository = inquiryRepository;
		this.userRepository = userRepository;
	}

	/** 임시보호 공고(앱 문의)여야 하고, 모집이 끝났거나 처리 중인 내 의사가 있으면 409. */
	public FosterInquiryResponse inquire(Long userId, Long postingId, FosterInquiryRequest request) {
		PostingRef posting = postingLookup.requireVisible(postingId);
		if (!PostingType.FOSTER.name().equals(posting.getPostingType())
				|| PostingQueryService.fosterMethod(posting) != FosterContactMethod.APP) {
			throw new BusinessException(PostingErrorCode.UNSUPPORTED_POSTING_TYPE);
		}
		if (!PostingRef.STATUS_OPEN.equals(posting.getStatus())) {
			throw new BusinessException(PostingErrorCode.POSTING_CLOSED);
		}
		if (inquiryRepository.existsByUserIdAndPostingIdAndStatus(userId, postingId,
				FosterInquiryStatus.RECEIVED)) {
			throw new BusinessException(PostingErrorCode.INQUIRY_ALREADY_RECEIVED);
		}

		String message = blankToNull(request == null ? null : request.message());
		String phone = blankToNull(request == null ? null : request.contactPhone());
		if (phone == null) {
			User user = userRepository.findById(userId)
					.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
			phone = user.getPhone();
		}
		return FosterInquiryResponse
				.from(inquiryRepository.saveAndFlush(FosterInquiry.receive(postingId, userId, message, phone)));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}

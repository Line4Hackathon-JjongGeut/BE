package com.jjonggeut.domain.posting.service;

import com.jjonggeut.domain.posting.dto.DonationLinkClickResponse;
import com.jjonggeut.domain.posting.entity.PostingType;
import com.jjonggeut.domain.posting.exception.PostingErrorCode;
import com.jjonggeut.domain.recommendation.dto.ActivityEventRequest;
import com.jjonggeut.domain.recommendation.dto.ActivityEventResponse;
import com.jjonggeut.domain.recommendation.entity.ActivityEventType;
import com.jjonggeut.domain.recommendation.entity.PostingRef;
import com.jjonggeut.domain.recommendation.service.ActivityEventService;
import com.jjonggeut.global.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 후원 링크 클릭을 행동 이벤트로 기록하고 이동할 공식 링크를 돌려준다. */
@Service
@Transactional
public class PostingDonationService {

	private final PostingLookup postingLookup;
	private final ActivityEventService activityEventService;

	public PostingDonationService(PostingLookup postingLookup, ActivityEventService activityEventService) {
		this.postingLookup = postingLookup;
		this.activityEventService = activityEventService;
	}

	/** 후원 공고이고 링크가 있어야 한다. 추천 항목 ID 는 내 추천이고 같은 공고일 때만 받는다. */
	public DonationLinkClickResponse record(Long userId, Long postingId, Long recommendationItemId) {
		PostingRef posting = postingLookup.requireVisible(postingId);
		if (!PostingType.SPONSOR.name().equals(posting.getPostingType())
				|| !PostingQueryService.hasDonationLink(posting)) {
			throw new BusinessException(PostingErrorCode.UNSUPPORTED_POSTING_TYPE);
		}
		ActivityEventResponse event = activityEventService.record(userId, new ActivityEventRequest(
				ActivityEventType.DONATION_LINK_CLICK, postingId, posting.getShelterId(), recommendationItemId,
				null));
		return new DonationLinkClickResponse(postingId, posting.getDonationUrl(), event.occurredAt());
	}
}

package com.jjonggeut.domain.posting.service;

import com.jjonggeut.domain.activity.dto.RegionResponse;
import com.jjonggeut.domain.activity.repository.PostingBookmarkRefRepository;
import com.jjonggeut.domain.posting.dto.PostingDetailResponse;
import com.jjonggeut.domain.posting.dto.PostingQuestionsResponse;
import com.jjonggeut.domain.posting.dto.PostingScheduleResponse;
import com.jjonggeut.domain.posting.entity.FosterContactMethod;
import com.jjonggeut.domain.posting.entity.PostingType;
import com.jjonggeut.domain.posting.exception.PostingErrorCode;
import com.jjonggeut.domain.posting.repository.PostingApplicationQuestionRepository;
import com.jjonggeut.domain.posting.repository.PostingMediaRepository;
import com.jjonggeut.domain.posting.repository.PostingScheduleRepository;
import com.jjonggeut.domain.recommendation.entity.PostingRef;
import com.jjonggeut.domain.recommendation.entity.ShelterRef;
import com.jjonggeut.domain.recommendation.repository.ShelterRefRepository;
import com.jjonggeut.domain.shelter.entity.VerificationStatus;
import com.jjonggeut.global.exception.BusinessException;
import com.jjonggeut.global.exception.ErrorCode;
import com.jjonggeut.global.util.GeoUtil;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공고 상세, 봉사 가능 일정, 봉사 신청 질문 조회. */
@Service
@Transactional(readOnly = true)
public class PostingQueryService {

	private final PostingLookup postingLookup;
	private final ShelterRefRepository shelterRepository;
	private final PostingBookmarkRefRepository bookmarkRepository;
	private final PostingScheduleRepository scheduleRepository;
	private final PostingApplicationQuestionRepository questionRepository;
	private final PostingMediaRepository mediaRepository;

	public PostingQueryService(PostingLookup postingLookup, ShelterRefRepository shelterRepository,
			PostingBookmarkRefRepository bookmarkRepository, PostingScheduleRepository scheduleRepository,
			PostingApplicationQuestionRepository questionRepository, PostingMediaRepository mediaRepository) {
		this.postingLookup = postingLookup;
		this.shelterRepository = shelterRepository;
		this.bookmarkRepository = bookmarkRepository;
		this.scheduleRepository = scheduleRepository;
		this.questionRepository = questionRepository;
		this.mediaRepository = mediaRepository;
	}

	public PostingDetailResponse detail(Long userId, Long postingId, Double latitude, Double longitude) {
		if ((latitude == null) != (longitude == null)
				|| latitude != null && (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		PostingRef posting = postingLookup.requireVisible(postingId);
		ShelterRef shelter = shelterRepository.findById(posting.getShelterId())
				.orElseThrow(() -> new BusinessException(PostingErrorCode.POSTING_NOT_FOUND));

		Double distance = null;
		if (latitude != null && shelter.getLatitude() != null && shelter.getLongitude() != null) {
			distance = Math.round(GeoUtil.haversineKm(latitude, longitude, shelter.getLatitude(),
					shelter.getLongitude()) * 10) / 10.0;
		}
		boolean verified = shelter.getVerificationStatus() == VerificationStatus.VERIFIED;
		RegionResponse region = shelter.getRegionSido() == null && shelter.getRegionSigungu() == null ? null
				: new RegionResponse(shelter.getRegionSido(), shelter.getRegionSigungu());
		PostingDetailResponse.ShelterInfo shelterInfo = new PostingDetailResponse.ShelterInfo(
				shelter.getShelterId(), shelter.getName(), region, shelter.getAddress(), shelter.getLatitude(),
				shelter.getLongitude(), verified ? shelter.getPhone() : null, shelter.getVerificationStatus());

		String type = posting.getPostingType();
		PostingDetailResponse.VolunteerInfo volunteer = null;
		PostingDetailResponse.SponsorInfo sponsor = null;
		PostingDetailResponse.FosterInfo foster = null;
		if (PostingType.VOLUNTEER.name().equals(type)) {
			volunteer = new PostingDetailResponse.VolunteerInfo(posting.getActivityCategories(),
					posting.getActivityStartAt(), posting.getActivityEndAt(), posting.getCapacity(),
					posting.getApplicantCount(),
					PostingLookup.remaining(posting.getCapacity(), posting.getApplicantCount()),
					new PostingDetailResponse.Conditions(posting.getMinAge(), posting.isBeginnerFriendly(),
							posting.getRequirements()));
		} else if (PostingType.SPONSOR.name().equals(type)) {
			sponsor = new PostingDetailResponse.SponsorInfo(posting.getDonationPurpose(), hasDonationLink(posting),
					posting.getDonationTargetQuantity(), posting.getDonationReceivedQuantity(),
					PostingLookup.donationProgressRate(posting.getDonationTargetQuantity(),
							posting.getDonationReceivedQuantity()));
		} else if (PostingType.FOSTER.name().equals(type)) {
			foster = new PostingDetailResponse.FosterInfo(fosterMethod(posting), posting.getExternalUrl());
		}

		boolean bookmarked = !bookmarkRepository.findBookmarkedPostingIds(userId, List.of(postingId)).isEmpty();
		List<String> imageUrls = mediaRepository.findByPostingIdInOrderByPostingIdAscPositionAsc(List.of(postingId))
				.stream().map(m -> m.getUrl()).toList();
		return new PostingDetailResponse(posting.getPostingId(), type, posting.getTitle(), posting.getStatus(),
				posting.isUrgent(), posting.getDescription(), posting.getAnimalType(), posting.getRecruitEndAt(),
				posting.getCreatedAt(), shelterInfo, distance, bookmarked, volunteer, sponsor, foster, imageUrls);
	}

	public PostingScheduleResponse schedules(Long postingId) {
		PostingRef posting = requireVolunteer(postingId);
		boolean open = PostingRef.STATUS_OPEN.equals(posting.getStatus());
		List<PostingScheduleResponse.Item> items = scheduleRepository
				.findUpcoming(posting.getPostingId(), LocalDateTime.now()).stream()
				.map(s -> new PostingScheduleResponse.Item(s.getScheduleId(), s.getStartAt(), s.getEndAt(),
						s.getCapacity(), s.getAppliedCount(), s.getRemainingCount(),
						open && s.getRemainingCount() > 0))
				.toList();
		return new PostingScheduleResponse(posting.getPostingId(), items);
	}

	public PostingQuestionsResponse questions(Long postingId) {
		PostingRef posting = requireVolunteer(postingId);
		List<PostingQuestionsResponse.Item> items = questionRepository
				.findByPostingIdOrderBySortOrderAscQuestionIdAsc(posting.getPostingId()).stream()
				.map(q -> new PostingQuestionsResponse.Item(q.getQuestionId(), q.getQuestionType(), q.getLabel(),
						q.isRequired(), q.getOptions(), q.getSortOrder()))
				.toList();
		return new PostingQuestionsResponse(posting.getPostingId(), items);
	}

	private PostingRef requireVolunteer(Long postingId) {
		PostingRef posting = postingLookup.requireVisible(postingId);
		if (!PostingType.VOLUNTEER.name().equals(posting.getPostingType())) {
			throw new BusinessException(PostingErrorCode.UNSUPPORTED_POSTING_TYPE);
		}
		return posting;
	}

	static boolean hasDonationLink(PostingRef posting) {
		return posting.getDonationUrl() != null && !posting.getDonationUrl().isBlank();
	}

	/** 문의 방식이 비어 있으면 앱에서 의사를 전달하는 방식으로 본다. */
	static FosterContactMethod fosterMethod(PostingRef posting) {
		return posting.getContactMethod() == null ? FosterContactMethod.APP : posting.getContactMethod();
	}
}

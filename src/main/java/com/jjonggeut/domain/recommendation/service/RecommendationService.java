package com.jjonggeut.domain.recommendation.service;

import com.jjonggeut.domain.consent.entity.ConsentType;
import com.jjonggeut.domain.consent.service.ConsentService;
import com.jjonggeut.domain.profile.entity.UserProfile;
import com.jjonggeut.domain.profile.repository.UserProfileRepository;
import com.jjonggeut.domain.recommendation.dto.RecommendationResponse;
import com.jjonggeut.domain.recommendation.entity.PostingRef;
import com.jjonggeut.domain.recommendation.entity.RecommendationBatch;
import com.jjonggeut.domain.recommendation.entity.RecommendationItem;
import com.jjonggeut.domain.recommendation.entity.RecommendationSource;
import com.jjonggeut.domain.recommendation.entity.ShelterRef;
import com.jjonggeut.domain.recommendation.repository.PostingRefRepository;
import com.jjonggeut.domain.recommendation.repository.RecommendationBatchRepository;
import com.jjonggeut.domain.recommendation.repository.RecommendationItemRepository;
import com.jjonggeut.domain.recommendation.repository.ShelterRefRepository;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import com.jjonggeut.domain.volunteerexperience.repository.VolunteerExperienceRepository;
import com.jjonggeut.global.exception.BusinessException;
import com.jjonggeut.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * AI 맞춤 활동 추천.
 * 후보 조회·규칙 점수 → (동의 시) AI 이유 생성 → 결과 저장 순서로 처리한다.
 * AI 호출 동안 DB 연결을 잡고 있지 않도록 조회와 저장을 각각 짧은 트랜잭션으로 나눈다.
 */
@Service
public class RecommendationService {

	static final int DEFAULT_LIMIT = 10;
	static final int MAX_LIMIT = 20;
	private static final int CANDIDATE_LIMIT = 500;

	private final UserProfileRepository profileRepository;
	private final VolunteerExperienceRepository experienceRepository;
	private final PostingRefRepository postingRepository;
	private final ShelterRefRepository shelterRepository;
	private final RecommendationBatchRepository batchRepository;
	private final RecommendationItemRepository itemRepository;
	private final ConsentService consentService;
	private final RuleScorer ruleScorer;
	private final RecommendationReasonGenerator reasonGenerator;
	private final TransactionTemplate transactionTemplate;

	public RecommendationService(UserProfileRepository profileRepository,
			VolunteerExperienceRepository experienceRepository, PostingRefRepository postingRepository,
			ShelterRefRepository shelterRepository, RecommendationBatchRepository batchRepository,
			RecommendationItemRepository itemRepository, ConsentService consentService, RuleScorer ruleScorer,
			RecommendationReasonGenerator reasonGenerator, PlatformTransactionManager transactionManager) {
		this.profileRepository = profileRepository;
		this.experienceRepository = experienceRepository;
		this.postingRepository = postingRepository;
		this.shelterRepository = shelterRepository;
		this.batchRepository = batchRepository;
		this.itemRepository = itemRepository;
		this.consentService = consentService;
		this.ruleScorer = ruleScorer;
		this.reasonGenerator = reasonGenerator;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	public RecommendationResponse recommend(Long userId, Integer limitParam, Double latitude, Double longitude) {
		int limit = validate(limitParam, latitude, longitude);

		Prepared prepared = transactionTemplate.execute(status -> prepare(userId, limit, latitude, longitude));

		Map<Long, String> aiReasons = prepared.aiAllowed()
				? reasonGenerator.generate(prepared.ranked())
				: Map.of();
		RecommendationSource source = aiReasons.isEmpty() ? RecommendationSource.RULE : RecommendationSource.AI;

		return transactionTemplate.execute(status -> save(userId, prepared.ranked(), aiReasons, source));
	}

	private int validate(Integer limitParam, Double latitude, Double longitude) {
		int limit = limitParam == null ? DEFAULT_LIMIT : limitParam;
		if (limit < 1 || limit > MAX_LIMIT) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		if ((latitude == null) != (longitude == null)) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		if (latitude != null && (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		return limit;
	}

	private Prepared prepare(Long userId, int limit, Double latitude, Double longitude) {
		ScoringContext ctx = buildContext(userId, latitude, longitude);

		List<PostingRef> postings = postingRepository.findOpenVolunteerPostings(LocalDateTime.now(),
				PageRequest.of(0, CANDIDATE_LIMIT));
		Map<Long, ShelterRef> shelters = shelterRepository
				.findAllById(postings.stream().map(PostingRef::getShelterId).collect(Collectors.toSet())).stream()
				.collect(Collectors.toMap(ShelterRef::getShelterId, Function.identity()));

		List<RecommendationCandidate> ranked = postings.stream()
				.map(posting -> ruleScorer.score(posting, shelters.get(posting.getShelterId()), ctx))
				.flatMap(Optional::stream)
				.sorted(Comparator.comparingDouble(RecommendationCandidate::score).reversed()
						.thenComparing(c -> c.posting().getPostingId(), Comparator.reverseOrder()))
				.limit(limit)
				.toList();

		boolean aiAllowed = !ranked.isEmpty() && consentService.isAgreed(userId, ConsentType.AI_PROCESSING);
		return new Prepared(ranked, aiAllowed);
	}

	/** 프로필이 없으면 거리·관심 활동 없이 시설 수요 등으로만 추천한다. 요청의 현재 위치가 프로필 위치보다 우선한다. */
	private ScoringContext buildContext(Long userId, Double latitude, Double longitude) {
		UserProfile profile = profileRepository.findByUserId(userId).orElse(null);

		Set<ActivityCategory> history = new HashSet<>();
		experienceRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDescExperienceIdDesc(userId)
				.forEach(experience -> history.addAll(experience.getActivityCategories()));

		Double lat = latitude;
		Double lon = longitude;
		if (lat == null && profile != null && profile.getLatitude() != null && profile.getLongitude() != null) {
			lat = profile.getLatitude();
			lon = profile.getLongitude();
		}

		if (profile == null) {
			return new ScoringContext(lat, lon, null, Set.of(), history, Set.of(), Set.of(), Set.of());
		}
		return new ScoringContext(lat, lon, profile.getMaxDistanceKm(),
				new HashSet<>(profile.getInterestCategories()), history,
				new HashSet<>(profile.getInterestAnimals()), new HashSet<>(profile.getAvailableDays()),
				new HashSet<>(profile.getAvailableTimeSlots()));
	}

	private RecommendationResponse save(Long userId, List<RecommendationCandidate> ranked,
			Map<Long, String> aiReasons, RecommendationSource source) {
		LocalDateTime now = LocalDateTime.now();
		if (ranked.isEmpty()) {
			return new RecommendationResponse(null, RecommendationSource.RULE, now, List.of());
		}

		RecommendationBatch batch = batchRepository.save(RecommendationBatch.create(userId, source));

		List<RecommendationItem> items = new ArrayList<>();
		for (int i = 0; i < ranked.size(); i++) {
			RecommendationCandidate c = ranked.get(i);
			String reason = aiReasons.getOrDefault(c.posting().getPostingId(), c.ruleReason());
			items.add(RecommendationItem.create(batch.getRecommendationBatchId(), c.posting().getPostingId(), i + 1,
					c.score(), reason, c.reasonTags()));
		}
		List<RecommendationItem> saved = itemRepository.saveAll(items);

		List<RecommendationResponse.Item> responseItems = new ArrayList<>();
		for (int i = 0; i < saved.size(); i++) {
			RecommendationCandidate c = ranked.get(i);
			RecommendationItem item = saved.get(i);
			ShelterRef shelter = c.shelter();
			responseItems.add(new RecommendationResponse.Item(
					item.getRecommendationItemId(),
					item.getRankNo(),
					new RecommendationResponse.PostingCard(
							c.posting().getPostingId(),
							c.posting().getTitle(),
							shelter == null ? null : shelter.getName(),
							shelter == null ? null
									: new RecommendationResponse.Region(shelter.getRegionSido(),
											shelter.getRegionSigungu()),
							c.distanceKm(),
							c.posting().getActivityCategories()),
					item.getReason(),
					item.getReasonTags()));
		}
		return new RecommendationResponse(batch.getRecommendationBatchId(), source, batch.getCreatedAt(),
				responseItems);
	}

	private record Prepared(List<RecommendationCandidate> ranked, boolean aiAllowed) {
	}
}

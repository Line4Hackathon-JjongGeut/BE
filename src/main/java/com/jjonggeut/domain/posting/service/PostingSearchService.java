package com.jjonggeut.domain.posting.service;

import com.jjonggeut.domain.activity.dto.PageResponse;
import com.jjonggeut.domain.activity.dto.RegionResponse;
import com.jjonggeut.domain.activity.repository.PostingBookmarkRefRepository;
import com.jjonggeut.domain.activity.service.PageParams;
import com.jjonggeut.domain.posting.dto.PostingListResponse;
import com.jjonggeut.domain.posting.dto.PostingShelterSummary;
import com.jjonggeut.domain.posting.dto.PostingSummaryItem;
import com.jjonggeut.domain.posting.entity.PostingMedia;
import com.jjonggeut.domain.posting.entity.PostingType;
import com.jjonggeut.domain.posting.repository.PostingMediaRepository;
import com.jjonggeut.domain.profile.entity.InterestAnimal;
import com.jjonggeut.domain.recommendation.entity.PostingRef;
import com.jjonggeut.domain.recommendation.entity.ShelterRef;
import com.jjonggeut.domain.recommendation.repository.PostingRefRepository;
import com.jjonggeut.domain.recommendation.repository.ShelterRefRepository;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import com.jjonggeut.global.exception.BusinessException;
import com.jjonggeut.global.exception.ErrorCode;
import com.jjonggeut.global.util.GeoUtil;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모집공고 검색과 긴급 공고 조회. 공고의 단순 조건은 DB 에서, 활동 분류·거리·정렬은 서버에서 처리한다.
 * 공고가 수백 건 수준이라는 전제라 모집 중인 공고를 메모리에서 거르고 나눈다.
 */
@Service
@Transactional(readOnly = true)
public class PostingSearchService {

	private static final int MAX_DISTANCE_KM = 200;
	private static final int MAX_KEYWORD_LENGTH = 50;
	private static final int DEFAULT_URGENT_LIMIT = 10;
	private static final int MAX_URGENT_LIMIT = 20;

	/** 정렬 기준. */
	public enum Sort {
		LATEST,
		START_SOON,
		DEADLINE,
		URGENT,
		DISTANCE
	}

	/** 검색 조건. 모두 선택이며 서로의 조합 규칙은 서비스가 확인한다. */
	public record Query(String keyword, String postingType, List<String> activityCategories, String animalType,
			String sido, String sigungu, Double latitude, Double longitude, Integer maxDistanceKm,
			LocalDate startDate, LocalDate endDate, Boolean beginnerFriendly, Boolean urgentOnly, String sort,
			Integer page, Integer size) {
	}

	private final PostingRefRepository postingRepository;
	private final ShelterRefRepository shelterRepository;
	private final PostingBookmarkRefRepository bookmarkRepository;
	private final PostingMediaRepository mediaRepository;

	public PostingSearchService(PostingRefRepository postingRepository, ShelterRefRepository shelterRepository,
			PostingBookmarkRefRepository bookmarkRepository, PostingMediaRepository mediaRepository) {
		this.postingRepository = postingRepository;
		this.shelterRepository = shelterRepository;
		this.bookmarkRepository = bookmarkRepository;
		this.mediaRepository = mediaRepository;
	}

	public PageResponse<PostingSummaryItem> search(Long userId, Query query) {
		Criteria criteria = Criteria.from(query);
		Pageable pageable = PageParams.of(query.page(), query.size());

		List<Candidate> candidates = candidates(criteria);
		int from = (int) Math.min((long) pageable.getPageNumber() * pageable.getPageSize(), candidates.size());
		int to = Math.min(from + pageable.getPageSize(), candidates.size());
		List<PostingSummaryItem> items = toItems(userId, candidates.subList(from, to));
		return PageResponse.of(new PageImpl<>(items, pageable, candidates.size()), Function.identity());
	}

	public PostingListResponse urgent(Long userId, String postingType, Integer limit) {
		int size = limit == null ? DEFAULT_URGENT_LIMIT : limit;
		if (size < 1 || size > MAX_URGENT_LIMIT) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		Criteria criteria = Criteria.from(new Query(null, postingType, null, null, null, null, null, null, null,
				null, null, null, true, Sort.URGENT.name(), null, null));
		List<Candidate> candidates = candidates(criteria);
		return new PostingListResponse(toItems(userId, candidates.subList(0, Math.min(size, candidates.size()))));
	}

	// ---- 후보 고르기 ----

	private List<Candidate> candidates(Criteria c) {
		Set<Long> regionShelterIds = null;
		if (c.sido() != null) {
			regionShelterIds = shelterRepository.findAll((root, q, builder) -> {
				List<Predicate> predicates = new ArrayList<>();
				predicates.add(builder.equal(root.get("regionSido"), c.sido()));
				if (c.sigungu() != null) {
					predicates.add(builder.equal(root.get("regionSigungu"), c.sigungu()));
				}
				return builder.and(predicates.toArray(new Predicate[0]));
			}).stream().map(ShelterRef::getShelterId).collect(Collectors.toSet());
			if (regionShelterIds.isEmpty()) {
				return List.of();
			}
		}

		List<PostingRef> postings = postingRepository.findAll(specification(c, regionShelterIds));
		Map<Long, ShelterRef> shelters = shelterRepository
				.findAllById(postings.stream().map(PostingRef::getShelterId).collect(Collectors.toSet())).stream()
				.collect(Collectors.toMap(ShelterRef::getShelterId, Function.identity()));

		List<Candidate> candidates = new ArrayList<>();
		for (PostingRef posting : postings) {
			ShelterRef shelter = shelters.get(posting.getShelterId());
			if (shelter == null) {
				continue;
			}
			if (!c.activityCategories().isEmpty()
					&& posting.getActivityCategories().stream().noneMatch(c.activityCategories()::contains)) {
				continue;
			}
			Double distance = null;
			if (c.hasOrigin() && shelter.getLatitude() != null && shelter.getLongitude() != null) {
				distance = GeoUtil.haversineKm(c.latitude(), c.longitude(), shelter.getLatitude(),
						shelter.getLongitude());
			}
			if (c.maxDistanceKm() != null && (distance == null || distance > c.maxDistanceKm())) {
				continue;
			}
			candidates.add(new Candidate(posting, shelter, distance));
		}
		candidates.sort(comparator(c.sort()));
		return candidates;
	}

	private Specification<PostingRef> specification(Criteria c, Set<Long> regionShelterIds) {
		return (root, q, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(builder.equal(root.get("status"), PostingRef.STATUS_OPEN));
			predicates.add(builder.or(builder.isNull(root.get("publicPosting")),
					builder.isTrue(root.get("publicPosting"))));
			if (regionShelterIds != null) {
				predicates.add(root.get("shelterId").in(regionShelterIds));
			}
			if (c.postingType() != null) {
				predicates.add(builder.equal(root.get("postingType"), c.postingType().name()));
			}
			if (c.animalType() != null) {
				predicates.add(builder.or(builder.equal(root.get("animalType"), c.animalType().name()),
						builder.equal(root.get("animalType"), "ALL")));
			}
			if (c.keyword() != null) {
				predicates.add(builder.like(builder.lower(root.get("title")), "%" + escapeLike(c.keyword()) + "%",
						LIKE_ESCAPE));
			}
			if (c.startDate() != null) {
				predicates.add(builder.greaterThanOrEqualTo(root.<LocalDateTime>get("activityStartAt"),
						c.startDate().atStartOfDay()));
			}
			if (c.endDate() != null) {
				predicates.add(builder.lessThan(root.<LocalDateTime>get("activityStartAt"),
						c.endDate().plusDays(1).atStartOfDay()));
			}
			if (c.beginnerFriendly()) {
				predicates.add(builder.or(builder.isNull(root.get("beginnerFriendly")),
						builder.isTrue(root.get("beginnerFriendly"))));
			}
			if (c.urgentOnly()) {
				predicates.add(builder.isTrue(root.get("urgent")));
			}
			return builder.and(predicates.toArray(new Predicate[0]));
		};
	}

	/** MySQL 에서 백슬래시는 문자열 이스케이프라 헷갈리지 않도록 느낌표를 LIKE 이스케이프 문자로 쓴다. */
	private static final char LIKE_ESCAPE = '!';

	private static String escapeLike(String keyword) {
		return keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
	}

	private Comparator<Candidate> comparator(Sort sort) {
		Comparator<Candidate> byNewest = Comparator.comparing((Candidate c) -> c.posting().getPostingId()).reversed();
		Comparator<Candidate> byStart = Comparator.comparing(c -> c.posting().getActivityStartAt(),
				Comparator.nullsLast(Comparator.naturalOrder()));
		Comparator<Candidate> byDeadline = Comparator.comparing(c -> c.posting().getRecruitEndAt(),
				Comparator.nullsLast(Comparator.naturalOrder()));
		return switch (sort) {
			case LATEST -> byNewest;
			case START_SOON -> byStart.thenComparing(byNewest);
			case DEADLINE -> byDeadline.thenComparing(byNewest);
			case URGENT -> Comparator.comparing((Candidate c) -> c.posting().isUrgent()).reversed()
					.thenComparing(Comparator.comparingInt((Candidate c) -> c.posting().getUrgentPriority())
							.reversed())
					.thenComparing(byDeadline).thenComparing(byNewest);
			case DISTANCE -> Comparator.comparing(Candidate::distance,
					Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(byNewest);
		};
	}

	// ---- 응답 만들기 ----

	private List<PostingSummaryItem> toItems(Long userId, List<Candidate> page) {
		List<Long> ids = page.stream().map(c -> c.posting().getPostingId()).toList();
		Set<Long> bookmarked = new HashSet<>();
		Map<Long, String> thumbnails = new java.util.HashMap<>();
		if (!ids.isEmpty()) {
			bookmarked.addAll(bookmarkRepository.findBookmarkedPostingIds(userId, ids));
			for (PostingMedia media : mediaRepository.findByPostingIdInOrderByPostingIdAscPositionAsc(ids)) {
				thumbnails.putIfAbsent(media.getPostingId(), media.getUrl());
			}
		}
		return page.stream().map(c -> {
			PostingRef p = c.posting();
			ShelterRef s = c.shelter();
			RegionResponse region = s.getRegionSido() == null && s.getRegionSigungu() == null ? null
					: new RegionResponse(s.getRegionSido(), s.getRegionSigungu());
			Double distance = c.distance() == null ? null : Math.round(c.distance() * 10) / 10.0;
			return new PostingSummaryItem(p.getPostingId(), p.getPostingType(), p.getTitle(), p.getStatus(),
					p.isUrgent(), new PostingShelterSummary(s.getShelterId(), s.getName(), region),
					p.getActivityCategories(), p.getAnimalType(), p.getActivityStartAt(), p.getRecruitEndAt(),
					p.getCapacity(), p.getApplicantCount(),
					PostingLookup.remaining(p.getCapacity(), p.getApplicantCount()), p.isBeginnerFriendly(),
					distance, bookmarked.contains(p.getPostingId()), thumbnails.get(p.getPostingId()));
		}).toList();
	}

	private record Candidate(PostingRef posting, ShelterRef shelter, Double distance) {
	}

	/** 검증을 마친 검색 조건. */
	private record Criteria(String keyword, PostingType postingType, Set<ActivityCategory> activityCategories,
			InterestAnimal animalType, String sido, String sigungu, Double latitude, Double longitude,
			Integer maxDistanceKm, LocalDate startDate, LocalDate endDate, boolean beginnerFriendly,
			boolean urgentOnly, Sort sort) {

		boolean hasOrigin() {
			return latitude != null;
		}

		static Criteria from(Query q) {
			String keyword = q.keyword() == null ? null : q.keyword().trim().toLowerCase();
			if (keyword != null && (keyword.isEmpty() || keyword.length() > MAX_KEYWORD_LENGTH)) {
				throw invalid();
			}
			String sido = trimmed(q.sido());
			String sigungu = trimmed(q.sigungu());
			if (q.sido() != null && sido == null || q.sigungu() != null && sigungu == null
					|| sigungu != null && sido == null) {
				throw invalid();
			}
			if ((q.latitude() == null) != (q.longitude() == null)) {
				throw invalid();
			}
			if (q.latitude() != null && (q.latitude() < -90 || q.latitude() > 90 || q.longitude() < -180
					|| q.longitude() > 180)) {
				throw invalid();
			}
			if (q.maxDistanceKm() != null
					&& (q.latitude() == null || q.maxDistanceKm() < 1 || q.maxDistanceKm() > MAX_DISTANCE_KM)) {
				throw invalid();
			}
			if (q.startDate() != null && q.endDate() != null && q.endDate().isBefore(q.startDate())) {
				throw invalid();
			}
			Sort sort = q.sort() == null ? Sort.LATEST : parse(Sort.class, q.sort());
			if (sort == Sort.DISTANCE && q.latitude() == null) {
				throw invalid();
			}
			Set<ActivityCategory> categories = new LinkedHashSet<>();
			if (q.activityCategories() != null) {
				for (String value : q.activityCategories()) {
					categories.add(parse(ActivityCategory.class, value));
				}
			}
			InterestAnimal animal = q.animalType() == null ? null : parse(InterestAnimal.class, q.animalType());
			PostingType type = q.postingType() == null ? null : parse(PostingType.class, q.postingType());
			return new Criteria(keyword, type, categories, animal, sido, sigungu, q.latitude(), q.longitude(),
					q.maxDistanceKm(), q.startDate(), q.endDate(), Boolean.TRUE.equals(q.beginnerFriendly()),
					Boolean.TRUE.equals(q.urgentOnly()), sort);
		}

		private static String trimmed(String value) {
			if (value == null) {
				return null;
			}
			String trimmed = value.trim();
			return trimmed.isEmpty() ? null : trimmed;
		}

		/** 정의되지 않은 값이면 400. 대소문자는 구분한다. */
		private static <E extends Enum<E>> E parse(Class<E> type, String value) {
			try {
				return Enum.valueOf(type, value);
			} catch (IllegalArgumentException e) {
				throw invalid();
			}
		}

		private static BusinessException invalid() {
			return new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
	}
}

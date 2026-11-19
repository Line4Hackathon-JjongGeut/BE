package com.jjonggeut.domain.shelter.service;

import com.jjonggeut.domain.activity.dto.PageResponse;
import com.jjonggeut.domain.activity.dto.RegionResponse;
import com.jjonggeut.domain.activity.repository.ShelterBookmarkRefRepository;
import com.jjonggeut.domain.activity.service.PageParams;
import com.jjonggeut.domain.posting.repository.PostingMediaRepository;
import com.jjonggeut.domain.profile.entity.InterestAnimal;
import com.jjonggeut.domain.recommendation.entity.PostingRef;
import com.jjonggeut.domain.recommendation.entity.ShelterRef;
import com.jjonggeut.domain.recommendation.repository.PostingRefRepository;
import com.jjonggeut.domain.recommendation.repository.ShelterRefRepository;
import com.jjonggeut.domain.shelter.dto.ShelterAnimalStatsResponse;
import com.jjonggeut.domain.shelter.dto.ShelterDetailResponse;
import com.jjonggeut.domain.shelter.dto.ShelterListItem;
import com.jjonggeut.domain.shelter.dto.ShelterOpenPostingItem;
import com.jjonggeut.domain.shelter.entity.VerificationStatus;
import com.jjonggeut.domain.shelter.entity.ShelterMedia;
import com.jjonggeut.domain.shelter.exception.ShelterErrorCode;
import com.jjonggeut.domain.shelter.repository.ShelterAnimalStatRefRepository;
import com.jjonggeut.domain.shelter.repository.ShelterMediaRepository;
import com.jjonggeut.global.exception.BusinessException;
import com.jjonggeut.global.exception.ErrorCode;
import com.jjonggeut.global.util.GeoUtil;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 보호시설 목록·상세 조회. 지역·인증 상태는 DB 에서, 동물 유형·거리는 서버에서 걸러 정렬하고 나눈다.
 * 시설이 수백 곳 수준이라는 전제라 전체를 메모리에서 거른다.
 */
@Service
@Transactional(readOnly = true)
public class ShelterQueryService {

	private static final int MAX_DISTANCE_KM = 200;
	private static final int DETAIL_POSTING_LIMIT = 10;

	private final ShelterRefRepository shelterRepository;
	private final PostingRefRepository postingRepository;
	private final ShelterBookmarkRefRepository bookmarkRepository;
	private final ShelterAnimalStatRefRepository animalStatRepository;
	private final ShelterMediaRepository mediaRepository;
	private final PostingMediaRepository postingMediaRepository;

	public ShelterQueryService(ShelterRefRepository shelterRepository, PostingRefRepository postingRepository,
			ShelterBookmarkRefRepository bookmarkRepository,
			ShelterAnimalStatRefRepository animalStatRepository, ShelterMediaRepository mediaRepository,
			PostingMediaRepository postingMediaRepository) {
		this.shelterRepository = shelterRepository;
		this.postingRepository = postingRepository;
		this.bookmarkRepository = bookmarkRepository;
		this.animalStatRepository = animalStatRepository;
		this.mediaRepository = mediaRepository;
		this.postingMediaRepository = postingMediaRepository;
	}

	/** 조회 조건. 모두 선택이며 서로의 조합 규칙은 {@link #validate()} 가 확인한다. */
	public record Query(String sido, String sigungu, Double latitude, Double longitude, Integer maxDistanceKm,
			String verificationStatus, String animalType, Integer page, Integer size) {

		private void validate() {
			if (sido != null && sido.isBlank() || sigungu != null && sigungu.isBlank()) {
				throw new BusinessException(ErrorCode.VALIDATION_FAILED);
			}
			if (sigungu != null && sido == null) {
				throw new BusinessException(ErrorCode.VALIDATION_FAILED);
			}
			if ((latitude == null) != (longitude == null)) {
				throw new BusinessException(ErrorCode.VALIDATION_FAILED);
			}
			if (latitude != null && (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)) {
				throw new BusinessException(ErrorCode.VALIDATION_FAILED);
			}
			if (maxDistanceKm != null && (latitude == null || maxDistanceKm < 1 || maxDistanceKm > MAX_DISTANCE_KM)) {
				throw new BusinessException(ErrorCode.VALIDATION_FAILED);
			}
		}
	}

	public PageResponse<ShelterListItem> list(Long userId, Query query) {
		query.validate();
		VerificationStatus verification = parseEnum(VerificationStatus.class, query.verificationStatus());
		InterestAnimal animal = parseEnum(InterestAnimal.class, query.animalType());
		Pageable pageable = PageParams.of(query.page(), query.size());

		List<ShelterRef> shelters = shelterRepository.findAll(
				specification(query, verification), Sort.by("name").and(Sort.by("shelterId")));

		boolean hasOrigin = query.latitude() != null;
		List<Candidate> candidates = new ArrayList<>();
		for (ShelterRef shelter : shelters) {
			if (animal != null && !shelter.getAnimalTypes().contains(animal)) {
				continue;
			}
			Double distance = null;
			if (hasOrigin && shelter.getLatitude() != null && shelter.getLongitude() != null) {
				distance = GeoUtil.haversineKm(query.latitude(), query.longitude(), shelter.getLatitude(),
						shelter.getLongitude());
			}
			if (query.maxDistanceKm() != null && (distance == null || distance > query.maxDistanceKm())) {
				continue;
			}
			candidates.add(new Candidate(shelter, distance));
		}
		if (hasOrigin) {
			// 가까운 순. 좌표가 없는 시설은 맨 뒤에 두고, 같은 거리는 이름순을 유지한다(안정 정렬).
			candidates.sort(Comparator.comparing(Candidate::distance, Comparator.nullsLast(Comparator.naturalOrder())));
		}

		int from = (int) Math.min((long) pageable.getPageNumber() * pageable.getPageSize(), candidates.size());
		int to = Math.min(from + pageable.getPageSize(), candidates.size());
		List<Candidate> pageItems = candidates.subList(from, to);

		List<Long> ids = pageItems.stream().map(c -> c.shelter().getShelterId()).toList();
		Map<Long, Long> openCounts = new HashMap<>();
		Set<Long> bookmarked = new HashSet<>();
		Map<Long, String> thumbnails = new HashMap<>();
		if (!ids.isEmpty()) {
			postingRepository.countOpenByShelterIds(ids).forEach(row -> openCounts.put((Long) row[0], (Long) row[1]));
			bookmarked.addAll(bookmarkRepository.findBookmarkedShelterIds(userId, ids));
			thumbnails.putAll(thumbnailsByShelterId(ids));
		}

		return PageResponse.of(new PageImpl<>(pageItems, pageable, candidates.size()), candidate -> {
			ShelterRef shelter = candidate.shelter();
			Double distance = candidate.distance() == null ? null : Math.round(candidate.distance() * 10) / 10.0;
			return new ShelterListItem(shelter.getShelterId(), shelter.getName(), region(shelter),
					shelter.getAddress(), shelter.getLatitude(), shelter.getLongitude(), shelter.getPhone(),
					shelter.getAnimalTypes(), shelter.getVerificationStatus(), distance,
					openCounts.getOrDefault(shelter.getShelterId(), 0L),
					bookmarked.contains(shelter.getShelterId()), thumbnails.get(shelter.getShelterId()));
		});
	}

	/** 시설별 대표(첫 번째) 사진 URL. */
	private Map<Long, String> thumbnailsByShelterId(List<Long> shelterIds) {
		Map<Long, String> result = new HashMap<>();
		for (ShelterMedia media : mediaRepository.findByShelterIdInOrderByShelterIdAscPositionAsc(shelterIds)) {
			result.putIfAbsent(media.getShelterId(), media.getUrl());
		}
		return result;
	}

	/** 시설 한 곳의 상세. 좌표를 함께 보내면 거리를 계산하고, 연락처는 인증된 시설만 내려준다. */
	public ShelterDetailResponse detail(Long userId, Long shelterId, Double latitude, Double longitude) {
		if (shelterId == null || shelterId < 1 || (latitude == null) != (longitude == null)
				|| latitude != null && (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		ShelterRef shelter = shelterRepository.findById(shelterId)
				.orElseThrow(() -> new BusinessException(ShelterErrorCode.SHELTER_NOT_FOUND));

		Double distance = null;
		if (latitude != null && shelter.getLatitude() != null && shelter.getLongitude() != null) {
			distance = Math.round(GeoUtil.haversineKm(latitude, longitude, shelter.getLatitude(),
					shelter.getLongitude()) * 10) / 10.0;
		}
		long openCount = postingRepository.countOpenByShelterIds(List.of(shelterId)).stream()
				.mapToLong(row -> (Long) row[1]).sum();
		List<PostingRef> openPostings = postingRepository.findOpenByShelterId(shelterId,
				PageRequest.of(0, DETAIL_POSTING_LIMIT));
		Map<Long, String> postingThumbnails = openPostings.isEmpty() ? Map.of()
				: postingMediaThumbnails(openPostings.stream().map(PostingRef::getPostingId).toList());
		List<ShelterOpenPostingItem> postings = openPostings.stream()
				.map(p -> new ShelterOpenPostingItem(p.getPostingId(), p.getPostingType(), p.getTitle(),
						p.getActivityStartAt(), p.getCapacity(), p.getApplicantCount(),
						postingThumbnails.get(p.getPostingId())))
				.toList();
		boolean bookmarked = !bookmarkRepository.findBookmarkedShelterIds(userId, List.of(shelterId)).isEmpty();
		List<String> imageUrls = mediaRepository.findByShelterIdInOrderByShelterIdAscPositionAsc(List.of(shelterId))
				.stream().map(ShelterMedia::getUrl).toList();

		return new ShelterDetailResponse(shelter.getShelterId(), shelter.getName(), region(shelter),
				shelter.getAddress(), shelter.getLatitude(), shelter.getLongitude(),
				shelter.getPhone(), shelter.getOperatingHours(), shelter.getVisitPolicy(),
				shelter.getDescription(), shelter.getAnimalTypes(), shelter.getVerificationStatus(), distance,
				bookmarked, openCount, postings, imageUrls);
	}

	/** 공고별 대표(첫 번째) 사진 URL. */
	private Map<Long, String> postingMediaThumbnails(List<Long> postingIds) {
		Map<Long, String> result = new HashMap<>();
		for (var media : postingMediaRepository.findByPostingIdInOrderByPostingIdAscPositionAsc(postingIds)) {
			result.putIfAbsent(media.getPostingId(), media.getUrl());
		}
		return result;
	}

	/** 시설의 보호동물 현황. 등록된 현황이 없는 시설은 모두 0 으로 준다. */
	public ShelterAnimalStatsResponse animalStats(Long shelterId) {
		if (shelterId == null || shelterId < 1) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		if (!shelterRepository.existsById(shelterId)) {
			throw new BusinessException(ShelterErrorCode.SHELTER_NOT_FOUND);
		}
		return animalStatRepository.findById(shelterId)
				.map(stat -> new ShelterAnimalStatsResponse(shelterId, stat.getDogCount(), stat.getCatCount(),
						stat.getOtherCount(), stat.getDogCount() + stat.getCatCount() + stat.getOtherCount(),
						stat.getUpdatedAt()))
				.orElseGet(() -> new ShelterAnimalStatsResponse(shelterId, 0, 0, 0, 0, null));
	}

	private Specification<ShelterRef> specification(Query query, VerificationStatus verification) {
		return (root, criteria, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (query.sido() != null) {
				predicates.add(builder.equal(root.get("regionSido"), query.sido().trim()));
			}
			if (query.sigungu() != null) {
				predicates.add(builder.equal(root.get("regionSigungu"), query.sigungu().trim()));
			}
			if (verification == VerificationStatus.VERIFIED) {
				predicates.add(builder.equal(root.get("verificationStatus"), VerificationStatus.VERIFIED));
			} else if (verification == VerificationStatus.UNVERIFIED) {
				// 값이 없는 시설도 미인증으로 본다.
				predicates.add(builder.or(
						builder.equal(root.get("verificationStatus"), VerificationStatus.UNVERIFIED),
						builder.isNull(root.get("verificationStatus"))));
			}
			if (query.maxDistanceKm() != null) {
				// 정확한 거리 계산 전에 좌표 범위로 후보를 줄인다.
				double latDelta = query.maxDistanceKm() / GeoUtil.KM_PER_LATITUDE_DEGREE;
				predicates.add(builder.between(root.<Double>get("latitude"), query.latitude() - latDelta,
						query.latitude() + latDelta));
				double cos = Math.cos(Math.toRadians(query.latitude()));
				if (cos > 0.01) {
					double lonDelta = query.maxDistanceKm() / (GeoUtil.KM_PER_LATITUDE_DEGREE * cos);
					predicates.add(builder.between(root.<Double>get("longitude"), query.longitude() - lonDelta,
							query.longitude() + lonDelta));
				}
			}
			return builder.and(predicates.toArray(new Predicate[0]));
		};
	}

	private RegionResponse region(ShelterRef shelter) {
		if (shelter.getRegionSido() == null && shelter.getRegionSigungu() == null) {
			return null;
		}
		return new RegionResponse(shelter.getRegionSido(), shelter.getRegionSigungu());
	}

	/** 값이 없으면 null, 정의되지 않은 값이면 400. 대소문자는 구분한다. */
	private <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
		if (value == null) {
			return null;
		}
		try {
			return Enum.valueOf(type, value);
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
	}

	private record Candidate(ShelterRef shelter, Double distance) {
	}
}

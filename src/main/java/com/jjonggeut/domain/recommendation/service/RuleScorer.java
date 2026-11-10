package com.jjonggeut.domain.recommendation.service;

import com.jjonggeut.domain.profile.entity.AvailableDay;
import com.jjonggeut.domain.profile.entity.AvailableTimeSlot;
import com.jjonggeut.domain.recommendation.entity.PostingRef;
import com.jjonggeut.domain.recommendation.entity.ShelterRef;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 규칙 기반 점수 계산. 사용자 조건이 있는 항목만 평균에 반영한다.
 * 거리 / 관심 활동(+봉사 경험) / 일정 / 관심 동물 / 시설 수요를 가중 평균한다.
 */
@Component
public class RuleScorer {

	static final String TAG_NEAR = "거리 가까움";
	static final String TAG_INTEREST = "관심 활동 일치";
	static final String TAG_HISTORY = "봉사 경험 연계";
	static final String TAG_SCHEDULE = "일정 일치";
	static final String TAG_ANIMAL = "관심 동물 일치";
	static final String TAG_DEMAND = "도움이 더 필요한 시설";

	private static final double W_DISTANCE = 0.25;
	private static final double W_CATEGORY = 0.25;
	private static final double W_SCHEDULE = 0.20;
	private static final double W_ANIMAL = 0.10;
	private static final double W_DEMAND = 0.20;

	/** 최대 거리 설정이 없을 때 거리 점수를 계산하는 기준 거리(km). */
	private static final double DEFAULT_RANGE_KM = 50.0;
	private static final double EARTH_RADIUS_KM = 6371.0;

	/** 거리 제한을 넘는 공고는 추천하지 않는다(빈 값). */
	public Optional<RecommendationCandidate> score(PostingRef posting, ShelterRef shelter, ScoringContext ctx) {
		double weightSum = 0;
		double weighted = 0;
		List<String> tags = new ArrayList<>();

		// 거리
		Double distanceKm = null;
		if (ctx.hasLocation() && shelter != null && shelter.getLatitude() != null && shelter.getLongitude() != null) {
			distanceKm = haversineKm(ctx.latitude(), ctx.longitude(), shelter.getLatitude(), shelter.getLongitude());
			if (ctx.maxDistanceKm() != null && distanceKm > ctx.maxDistanceKm()) {
				return Optional.empty();
			}
			double range = ctx.maxDistanceKm() != null ? ctx.maxDistanceKm() : DEFAULT_RANGE_KM;
			weighted += W_DISTANCE * Math.max(0, 1 - distanceKm / range);
			weightSum += W_DISTANCE;
			if (distanceKm <= range * 0.5) {
				tags.add(TAG_NEAR);
			}
		}

		// 관심 활동 + 봉사 경험
		boolean hasInterest = !ctx.interestCategories().isEmpty();
		boolean hasHistory = !ctx.historyCategories().isEmpty();
		if (hasInterest || hasHistory) {
			boolean interestMatch = overlaps(posting.getActivityCategories(), ctx.interestCategories());
			boolean historyMatch = overlaps(posting.getActivityCategories(), ctx.historyCategories());
			double s;
			if (hasInterest) {
				s = (interestMatch ? 0.8 : 0) + (historyMatch ? 0.2 : 0);
			} else {
				s = historyMatch ? 1.0 : 0;
			}
			weighted += W_CATEGORY * s;
			weightSum += W_CATEGORY;
			if (interestMatch) {
				tags.add(TAG_INTEREST);
			} else if (historyMatch) {
				tags.add(TAG_HISTORY);
			}
		}

		// 일정
		LocalDateTime start = posting.getActivityStartAt();
		boolean hasDays = !ctx.availableDays().isEmpty();
		boolean hasSlots = !ctx.availableTimeSlots().isEmpty();
		if (start != null && (hasDays || hasSlots)) {
			int checks = 0;
			int passed = 0;
			if (hasDays) {
				checks++;
				if (ctx.availableDays().contains(dayOf(start))) {
					passed++;
				}
			}
			if (hasSlots) {
				checks++;
				if (ctx.availableTimeSlots().contains(slotOf(start))) {
					passed++;
				}
			}
			double s = (double) passed / checks;
			weighted += W_SCHEDULE * s;
			weightSum += W_SCHEDULE;
			if (passed == checks) {
				tags.add(TAG_SCHEDULE);
			}
		}

		// 관심 동물
		if (!ctx.interestAnimals().isEmpty() && posting.getAnimalType() != null) {
			boolean match = "ALL".equalsIgnoreCase(posting.getAnimalType())
					|| ctx.interestAnimals().stream().anyMatch(a -> a.name().equalsIgnoreCase(posting.getAnimalType()));
			weighted += W_ANIMAL * (match ? 1 : 0);
			weightSum += W_ANIMAL;
			if (match) {
				tags.add(TAG_ANIMAL);
			}
		}

		// 시설 수요: 모집 인원 대비 신청이 적을수록 도움이 더 필요하다.
		if (posting.getCapacity() != null && posting.getCapacity() > 0 && posting.getApplicantCount() != null) {
			double need = 1 - Math.min(1.0, (double) posting.getApplicantCount() / posting.getCapacity());
			weighted += W_DEMAND * need;
			weightSum += W_DEMAND;
			if (need >= 0.6) {
				tags.add(TAG_DEMAND);
			}
		}

		double score = weightSum == 0 ? 0.5 : weighted / weightSum;
		Double rounded = distanceKm == null ? null : Math.round(distanceKm * 10) / 10.0;
		return Optional.of(new RecommendationCandidate(posting, shelter, rounded, score, List.copyOf(tags),
				ruleReason(tags)));
	}

	private String ruleReason(List<String> tags) {
		if (tags.isEmpty()) {
			return "지금 모집 중인 봉사예요.";
		}
		return String.join(" · ", tags.subList(0, Math.min(3, tags.size()))) + " 기준으로 추천해요.";
	}

	private boolean overlaps(List<ActivityCategory> categories, java.util.Set<ActivityCategory> wanted) {
		return categories != null && categories.stream().anyMatch(wanted::contains);
	}

	static AvailableDay dayOf(LocalDateTime time) {
		return AvailableDay.valueOf(time.getDayOfWeek().name().substring(0, 3));
	}

	static AvailableTimeSlot slotOf(LocalDateTime time) {
		int hour = time.getHour();
		if (hour < 12) {
			return AvailableTimeSlot.MORNING;
		}
		return hour < 18 ? AvailableTimeSlot.AFTERNOON : AvailableTimeSlot.EVENING;
	}

	static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
				* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}
}

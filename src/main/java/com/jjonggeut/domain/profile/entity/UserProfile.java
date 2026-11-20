package com.jjonggeut.domain.profile.entity;

import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategoryListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** 맞춤 추천에 쓰는 사용자 프로필(온보딩 입력). 사용자당 1행. */
@Entity
@Table(name = "user_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_profile_id")
	private Long userProfileId;

	@Column(name = "user_id", nullable = false, unique = true)
	private Long userId;

	@Column(name = "region_sido", length = 50)
	private String regionSido;

	@Column(name = "region_sigungu", length = 50)
	private String regionSigungu;

	@Column(name = "latitude")
	private Double latitude;

	@Column(name = "longitude")
	private Double longitude;

	@Column(name = "max_distance_km")
	private Integer maxDistanceKm;

	@Enumerated(EnumType.STRING)
	@Column(name = "experience_level", length = 20)
	private ExperienceLevel experienceLevel;

	@Enumerated(EnumType.STRING)
	@Column(name = "activity_frequency", length = 20)
	private ActivityFrequency activityFrequency;

	@Convert(converter = InterestAnimalListConverter.class)
	@Column(name = "interest_animals", nullable = false, length = 255)
	private List<InterestAnimal> interestAnimals;

	@Convert(converter = ActivityCategoryListConverter.class)
	@Column(name = "interest_categories", nullable = false, length = 255)
	private List<ActivityCategory> interestCategories;

	@Convert(converter = AvailableDayListConverter.class)
	@Column(name = "available_days", nullable = false, length = 255)
	private List<AvailableDay> availableDays;

	@Convert(converter = AvailableTimeSlotListConverter.class)
	@Column(name = "available_time_slots", nullable = false, length = 255)
	private List<AvailableTimeSlot> availableTimeSlots;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	/** 아직 아무 항목도 입력하지 않은 빈 프로필. 이후 change* 메서드로 채운다. */
	public static UserProfile empty(Long userId) {
		return create(userId, null, null, null, null, null, null, null,
				List.of(), List.of(), List.of(), List.of());
	}

	public static UserProfile create(Long userId, String regionSido, String regionSigungu, Double latitude,
			Double longitude, Integer maxDistanceKm, ExperienceLevel experienceLevel,
			ActivityFrequency activityFrequency, List<InterestAnimal> interestAnimals,
			List<ActivityCategory> interestCategories, List<AvailableDay> availableDays,
			List<AvailableTimeSlot> availableTimeSlots) {
		UserProfile profile = new UserProfile();
		profile.userId = userId;
		profile.regionSido = regionSido;
		profile.regionSigungu = regionSigungu;
		profile.latitude = latitude;
		profile.longitude = longitude;
		profile.maxDistanceKm = maxDistanceKm;
		profile.experienceLevel = experienceLevel;
		profile.activityFrequency = activityFrequency;
		profile.interestAnimals = interestAnimals;
		profile.interestCategories = interestCategories;
		profile.availableDays = availableDays;
		profile.availableTimeSlots = availableTimeSlots;
		return profile;
	}

	public void changeRegion(String regionSido, String regionSigungu, Double latitude, Double longitude) {
		this.regionSido = regionSido;
		this.regionSigungu = regionSigungu;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public void changeMaxDistanceKm(Integer maxDistanceKm) {
		this.maxDistanceKm = maxDistanceKm;
	}

	public void changeExperienceLevel(ExperienceLevel experienceLevel) {
		this.experienceLevel = experienceLevel;
	}

	public void changeActivityFrequency(ActivityFrequency activityFrequency) {
		this.activityFrequency = activityFrequency;
	}

	public void changeInterestAnimals(List<InterestAnimal> interestAnimals) {
		this.interestAnimals = interestAnimals;
	}

	public void changeInterestCategories(List<ActivityCategory> interestCategories) {
		this.interestCategories = interestCategories;
	}

	public void changeAvailableDays(List<AvailableDay> availableDays) {
		this.availableDays = availableDays;
	}

	public void changeAvailableTimeSlots(List<AvailableTimeSlot> availableTimeSlots) {
		this.availableTimeSlots = availableTimeSlots;
	}
}

package com.jjonggeut.domain.savedsearch.entity;

import com.jjonggeut.domain.profile.entity.AvailableDay;
import com.jjonggeut.domain.profile.entity.AvailableDayListConverter;
import com.jjonggeut.domain.profile.entity.AvailableTimeSlot;
import com.jjonggeut.domain.profile.entity.AvailableTimeSlotListConverter;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategoryListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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

/** 사용자가 이름을 붙여 저장해 둔 활동 검색 조건. 사용자당 여러 개, 그중 기본은 최대 1개. */
@Entity
@Table(name = "saved_search")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedSearch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "saved_search_id")
	private Long savedSearchId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "name", nullable = false, length = 20)
	private String name;

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

	@Convert(converter = AvailableDayListConverter.class)
	@Column(name = "available_days", nullable = false, length = 255)
	private List<AvailableDay> availableDays;

	@Convert(converter = AvailableTimeSlotListConverter.class)
	@Column(name = "available_time_slots", nullable = false, length = 255)
	private List<AvailableTimeSlot> availableTimeSlots;

	@Convert(converter = ActivityCategoryListConverter.class)
	@Column(name = "activity_categories", nullable = false, length = 255)
	private List<ActivityCategory> activityCategories;

	@Column(name = "is_default", nullable = false)
	private boolean defaultSearch;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	public static SavedSearch create(Long userId, String name, String regionSido, String regionSigungu,
			Double latitude, Double longitude, Integer maxDistanceKm, List<AvailableDay> availableDays,
			List<AvailableTimeSlot> availableTimeSlots, List<ActivityCategory> activityCategories,
			boolean defaultSearch) {
		SavedSearch search = new SavedSearch();
		search.userId = userId;
		search.name = name;
		search.regionSido = regionSido;
		search.regionSigungu = regionSigungu;
		search.latitude = latitude;
		search.longitude = longitude;
		search.maxDistanceKm = maxDistanceKm;
		search.availableDays = availableDays;
		search.availableTimeSlots = availableTimeSlots;
		search.activityCategories = activityCategories;
		search.defaultSearch = defaultSearch;
		return search;
	}

	public void changeName(String name) {
		this.name = name;
	}

	public void changeRegion(String sido, String sigungu, Double latitude, Double longitude) {
		this.regionSido = sido;
		this.regionSigungu = sigungu;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public void changeMaxDistanceKm(Integer maxDistanceKm) {
		this.maxDistanceKm = maxDistanceKm;
	}

	public void changeAvailableDays(List<AvailableDay> availableDays) {
		this.availableDays = availableDays;
	}

	public void changeAvailableTimeSlots(List<AvailableTimeSlot> availableTimeSlots) {
		this.availableTimeSlots = availableTimeSlots;
	}

	public void changeActivityCategories(List<ActivityCategory> activityCategories) {
		this.activityCategories = activityCategories;
	}

	public void changeDefaultSearch(boolean defaultSearch) {
		this.defaultSearch = defaultSearch;
	}

	/** 지역·거리·요일·시간·활동 유형 중 하나라도 지정되어 있는지. */
	public boolean hasAnyFilter() {
		return regionSido != null || regionSigungu != null || maxDistanceKm != null
				|| !availableDays.isEmpty() || !availableTimeSlots.isEmpty() || !activityCategories.isEmpty();
	}
}

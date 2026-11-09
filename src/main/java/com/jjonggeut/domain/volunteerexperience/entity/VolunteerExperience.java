package com.jjonggeut.domain.volunteerexperience.entity;

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

@Entity
@Table(name = "user_volunteer_experience")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VolunteerExperience {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_volunteer_experience_id")
	private Long experienceId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Convert(converter = ActivityCategoryListConverter.class)
	@Column(name = "activity_categories", nullable = false, length = 255)
	private List<ActivityCategory> activityCategories;

	@Column(name = "organization_name", length = 100)
	private String organizationName;

	@Column(name = "period_text", length = 100)
	private String periodText;

	@Column(name = "content_raw", nullable = false, columnDefinition = "TEXT")
	private String contentRaw;

	@Column(name = "content_summary", columnDefinition = "TEXT")
	private String contentSummary;

	@Enumerated(EnumType.STRING)
	@Column(name = "summary_status", nullable = false, length = 20)
	private SummaryStatus summaryStatus;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public static VolunteerExperience create(Long userId, List<ActivityCategory> activityCategories,
			String organizationName, String periodText, String contentRaw, String contentSummary) {
		VolunteerExperience experience = new VolunteerExperience();
		experience.userId = userId;
		experience.activityCategories = activityCategories;
		experience.organizationName = organizationName;
		experience.periodText = periodText;
		experience.contentRaw = contentRaw;
		experience.contentSummary = contentSummary;
		experience.summaryStatus = contentSummary == null ? SummaryStatus.NONE : SummaryStatus.COMPLETED;
		return experience;
	}

	public void delete() {
		this.deletedAt = LocalDateTime.now();
	}
}

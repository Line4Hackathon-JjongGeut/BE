package com.jjonggeut.domain.volunteerexperience.dto;

import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import com.jjonggeut.domain.volunteerexperience.entity.SummaryStatus;
import com.jjonggeut.domain.volunteerexperience.entity.VolunteerExperience;
import java.time.LocalDateTime;
import java.util.List;

public record ExperienceResponse(
		Long experienceId,
		String content,
		String summary,
		SummaryStatus summaryStatus,
		List<ActivityCategory> activityCategories,
		String organizationName,
		String periodText,
		LocalDateTime createdAt
) {

	public static ExperienceResponse from(VolunteerExperience experience) {
		return new ExperienceResponse(
				experience.getExperienceId(),
				experience.getContentRaw(),
				experience.getContentSummary(),
				experience.getSummaryStatus(),
				experience.getActivityCategories(),
				experience.getOrganizationName(),
				experience.getPeriodText(),
				experience.getCreatedAt());
	}
}

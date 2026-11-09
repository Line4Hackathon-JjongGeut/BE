package com.jjonggeut.domain.volunteerexperience.dto;

import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import com.jjonggeut.domain.volunteerexperience.entity.SummaryStatus;
import java.util.List;

public record ExperienceSummaryResponse(
		SummaryStatus status,
		String summary,
		List<ActivityCategory> activityCategories,
		String organizationName,
		String periodText
) {

	public static ExperienceSummaryResponse completed(String summary, List<ActivityCategory> activityCategories,
			String organizationName, String periodText) {
		return new ExperienceSummaryResponse(SummaryStatus.COMPLETED, summary, activityCategories,
				organizationName, periodText);
	}

	/** AI 호출·파싱 실패. 앱은 사용자가 쓴 원문을 그대로 저장하도록 안내한다. */
	public static ExperienceSummaryResponse failed() {
		return new ExperienceSummaryResponse(SummaryStatus.FAILED, null, List.of(), null, null);
	}
}

package com.jjonggeut.domain.volunteerexperience.dto;

import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ExperienceCreateRequest(

		@NotBlank(message = "봉사 경험 내용을 입력해주세요.")
		@Size(max = 1000, message = "봉사 경험 내용은 1000자 이하로 입력해주세요.")
		String content,

		@Size(max = 1000, message = "정리된 내용은 1000자 이하로 입력해주세요.")
		String summary,

		@NotEmpty(message = "활동 분류를 1개 이상 선택해주세요.")
		@Size(max = 7, message = "활동 분류는 최대 7개까지 선택할 수 있습니다.")
		List<ActivityCategory> activityCategories,

		@Size(max = 100, message = "단체·시설명은 100자 이하로 입력해주세요.")
		String organizationName,

		@Size(max = 100, message = "기간은 100자 이하로 입력해주세요.")
		String periodText
) {
}

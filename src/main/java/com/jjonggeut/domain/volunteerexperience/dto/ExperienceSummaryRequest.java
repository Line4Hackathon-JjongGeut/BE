package com.jjonggeut.domain.volunteerexperience.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExperienceSummaryRequest(

		@NotBlank(message = "봉사 경험 내용을 입력해주세요.")
		@Size(max = 1000, message = "봉사 경험 내용은 1000자 이하로 입력해주세요.")
		String content
) {
}

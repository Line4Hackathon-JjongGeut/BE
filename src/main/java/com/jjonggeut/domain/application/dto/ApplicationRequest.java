package com.jjonggeut.domain.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 봉사 신청 요청. 둘 다 선택이지만 공고에 필수 질문이 있으면 answers 에 답해야 한다. */
public record ApplicationRequest(
		@Valid
		@Size(max = 50, message = "답변은 50개 이하로 보내주세요.")
		List<@NotNull(message = "답변 항목이 비어 있습니다.") Answer> answers,
		@Size(max = 500, message = "전할 말은 500자 이하로 입력해주세요.")
		String message) {

	public record Answer(
			@NotNull(message = "질문 ID 는 필수입니다.")
			@Positive(message = "질문 ID 는 양수여야 합니다.")
			Long questionId,
			@NotEmpty(message = "답변을 입력해주세요.")
			@Size(max = 20, message = "답변은 20개 이하로 보내주세요.")
			List<@NotBlank(message = "답변이 비어 있습니다.") @Size(max = 500, message = "답변은 500자 이하로 입력해주세요.") String> values) {
	}
}

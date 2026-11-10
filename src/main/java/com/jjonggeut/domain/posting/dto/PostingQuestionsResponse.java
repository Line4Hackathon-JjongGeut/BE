package com.jjonggeut.domain.posting.dto;

import com.jjonggeut.domain.posting.entity.QuestionType;
import java.util.List;

/** 봉사 신청서에 표시할 질문. options 는 선택형 질문의 선택지이고 서술형은 빈 배열이다. */
public record PostingQuestionsResponse(Long postingId, List<Item> items) {

	public record Item(
			Long questionId,
			QuestionType questionType,
			String label,
			boolean required,
			List<String> options,
			int sortOrder) {
	}
}

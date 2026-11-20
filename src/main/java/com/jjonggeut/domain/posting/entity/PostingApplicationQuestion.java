package com.jjonggeut.domain.posting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** 봉사 신청서에 표시할 질문(posting_application_question). 조회 전용이며 of() 는 테스트용이다. */
@Entity
@Immutable
@Table(name = "posting_application_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostingApplicationQuestion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "question_id")
	private Long questionId;

	@Column(name = "posting_id", nullable = false)
	private Long postingId;

	@Enumerated(EnumType.STRING)
	@Column(name = "question_type", nullable = false, length = 20)
	private QuestionType questionType;

	@Column(name = "label", nullable = false, length = 255)
	private String label;

	@Column(name = "is_required", nullable = false)
	private boolean required;

	@Convert(converter = StringListConverter.class)
	@Column(name = "options", length = 1000)
	private List<String> options;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	public static PostingApplicationQuestion of(Long postingId, QuestionType questionType, String label,
			boolean required, List<String> options, int sortOrder) {
		PostingApplicationQuestion question = new PostingApplicationQuestion();
		question.postingId = postingId;
		question.questionType = questionType;
		question.label = label;
		question.required = required;
		question.options = options;
		question.sortOrder = sortOrder;
		return question;
	}

	public List<String> getOptions() {
		return options == null ? List.of() : options;
	}
}

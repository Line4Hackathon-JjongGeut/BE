package com.jjonggeut.domain.activity.entity;

import java.util.List;

/** 봉사 신청서에 남긴 질문 하나의 답변. 선택형은 선택지 문자열을 그대로 values 에 담는다. */
public record ApplicationAnswer(Long questionId, List<String> values) {
}

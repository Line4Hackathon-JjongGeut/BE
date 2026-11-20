package com.jjonggeut.domain.activity.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

/** 신청서 답변 목록을 JSON 문자열로 저장한다. 값이 없으면 NULL 로 둔다. */
@Converter
public class AnswerListConverter implements AttributeConverter<List<ApplicationAnswer>, String> {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(List<ApplicationAnswer> attribute) {
		if (attribute == null) {
			return null;
		}
		try {
			return OBJECT_MAPPER.writeValueAsString(attribute);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("신청서 답변을 저장할 수 없습니다.", e);
		}
	}

	@Override
	public List<ApplicationAnswer> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isBlank()) {
			return null;
		}
		try {
			return OBJECT_MAPPER.readValue(dbData, new TypeReference<List<ApplicationAnswer>>() {
			});
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("신청서 답변을 읽을 수 없습니다.", e);
		}
	}
}

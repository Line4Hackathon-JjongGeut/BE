package com.jjonggeut.domain.volunteerexperience.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

/** 활동 분류 목록을 JSON 배열 문자열(예: ["WALK","CLEANING"])로 저장한다. MySQL JSON 컬럼과 H2 모두에서 동작한다. */
@Converter
public class ActivityCategoryListConverter implements AttributeConverter<List<ActivityCategory>, String> {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(List<ActivityCategory> attribute) {
		try {
			return OBJECT_MAPPER.writeValueAsString(attribute == null ? List.of() : attribute);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("활동 분류를 저장할 수 없습니다.", e);
		}
	}

	@Override
	public List<ActivityCategory> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isBlank()) {
			return List.of();
		}
		try {
			return OBJECT_MAPPER.readValue(dbData, new TypeReference<List<ActivityCategory>>() {
			});
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("활동 분류를 읽을 수 없습니다.", e);
		}
	}
}

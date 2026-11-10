package com.jjonggeut.domain.posting.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

/** 문자열 목록을 JSON 배열 문자열로 저장한다. */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(List<String> attribute) {
		try {
			return OBJECT_MAPPER.writeValueAsString(attribute == null ? List.of() : attribute);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("목록을 저장할 수 없습니다.", e);
		}
	}

	@Override
	public List<String> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isBlank()) {
			return List.of();
		}
		try {
			return OBJECT_MAPPER.readValue(dbData, new TypeReference<List<String>>() {
			});
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("목록을 읽을 수 없습니다.", e);
		}
	}
}

package com.jjonggeut.domain.profile.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import java.util.List;

/** enum 목록을 JSON 배열 문자열(예: ["DOG","CAT"])로 저장한다. MySQL JSON 컬럼과 H2 모두에서 동작한다. */
public abstract class EnumListConverter<E extends Enum<E>> implements AttributeConverter<List<E>, String> {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final Class<E> type;

	protected EnumListConverter(Class<E> type) {
		this.type = type;
	}

	@Override
	public String convertToDatabaseColumn(List<E> attribute) {
		try {
			return OBJECT_MAPPER.writeValueAsString(attribute == null ? List.of() : attribute);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("목록을 저장할 수 없습니다.", e);
		}
	}

	@Override
	public List<E> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isBlank()) {
			return List.of();
		}
		try {
			return OBJECT_MAPPER.readValue(dbData,
					OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, type));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("목록을 읽을 수 없습니다.", e);
		}
	}
}

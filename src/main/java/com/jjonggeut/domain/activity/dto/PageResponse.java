package com.jjonggeut.domain.activity.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** 페이지 번호는 0부터 시작한다. */
public record PageResponse<T>(
		List<T> items,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean hasNext
) {

	public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
		return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
				page.getSize(), page.getTotalElements(), page.getTotalPages(), page.hasNext());
	}
}

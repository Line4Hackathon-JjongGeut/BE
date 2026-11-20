package com.jjonggeut.domain.activity.service;

import com.jjonggeut.global.exception.BusinessException;
import com.jjonggeut.global.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** 목록 조회의 page(0부터, 기본 0), size(1~50, 기본 20) 검증. */
public final class PageParams {

	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 50;

	private PageParams() {
	}

	public static Pageable of(Integer page, Integer size) {
		int resolvedPage = page == null ? 0 : page;
		int resolvedSize = size == null ? DEFAULT_SIZE : size;
		if (resolvedPage < 0 || resolvedSize < 1 || resolvedSize > MAX_SIZE) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		return PageRequest.of(resolvedPage, resolvedSize);
	}
}

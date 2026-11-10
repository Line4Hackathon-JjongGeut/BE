package com.jjonggeut.domain.shelter.service;

import com.jjonggeut.domain.activity.entity.ShelterBookmarkRef;
import com.jjonggeut.domain.activity.repository.ShelterBookmarkRefRepository;
import com.jjonggeut.domain.recommendation.repository.ShelterRefRepository;
import com.jjonggeut.domain.shelter.dto.ShelterBookmarkResponse;
import com.jjonggeut.domain.shelter.exception.ShelterErrorCode;
import com.jjonggeut.global.exception.BusinessException;
import com.jjonggeut.global.exception.ErrorCode;
import java.time.LocalDateTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관심 시설 저장·해제. */
@Service
@Transactional
public class ShelterBookmarkService {

	private final ShelterRefRepository shelterRepository;
	private final ShelterBookmarkRefRepository bookmarkRepository;

	public ShelterBookmarkService(ShelterRefRepository shelterRepository,
			ShelterBookmarkRefRepository bookmarkRepository) {
		this.shelterRepository = shelterRepository;
		this.bookmarkRepository = bookmarkRepository;
	}

	/** 이미 저장한 시설이면 409. 동시에 두 번 저장되면 DB 유니크 제약이 막고 같은 409 로 돌려준다. */
	public ShelterBookmarkResponse save(Long userId, Long shelterId) {
		requireShelter(shelterId);
		if (bookmarkRepository.existsByUserIdAndShelterId(userId, shelterId)) {
			throw new BusinessException(ShelterErrorCode.BOOKMARK_ALREADY_EXISTS);
		}
		LocalDateTime now = LocalDateTime.now();
		try {
			bookmarkRepository.saveAndFlush(ShelterBookmarkRef.of(userId, shelterId, now));
		} catch (DataIntegrityViolationException e) {
			throw new BusinessException(ShelterErrorCode.BOOKMARK_ALREADY_EXISTS);
		}
		return new ShelterBookmarkResponse(shelterId, true, now);
	}

	/** 저장하지 않은 시설이면 404. */
	public void remove(Long userId, Long shelterId) {
		requireShelter(shelterId);
		if (bookmarkRepository.deleteByUserIdAndShelterId(userId, shelterId) == 0) {
			throw new BusinessException(ShelterErrorCode.BOOKMARK_NOT_FOUND);
		}
	}

	private void requireShelter(Long shelterId) {
		if (shelterId == null || shelterId < 1) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		if (!shelterRepository.existsById(shelterId)) {
			throw new BusinessException(ShelterErrorCode.SHELTER_NOT_FOUND);
		}
	}
}

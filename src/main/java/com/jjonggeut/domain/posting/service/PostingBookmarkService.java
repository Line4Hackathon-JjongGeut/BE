package com.jjonggeut.domain.posting.service;

import com.jjonggeut.domain.activity.entity.PostingBookmarkRef;
import com.jjonggeut.domain.activity.repository.PostingBookmarkRefRepository;
import com.jjonggeut.domain.posting.dto.PostingBookmarkResponse;
import com.jjonggeut.domain.posting.exception.PostingErrorCode;
import com.jjonggeut.global.exception.BusinessException;
import java.time.LocalDateTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관심 공고 저장·해제. */
@Service
@Transactional
public class PostingBookmarkService {

	private final PostingLookup postingLookup;
	private final PostingBookmarkRefRepository bookmarkRepository;

	public PostingBookmarkService(PostingLookup postingLookup, PostingBookmarkRefRepository bookmarkRepository) {
		this.postingLookup = postingLookup;
		this.bookmarkRepository = bookmarkRepository;
	}

	/** 이미 저장한 공고면 409. 동시에 두 번 저장되면 DB 유니크 제약이 막고 같은 409 로 돌려준다. */
	public PostingBookmarkResponse save(Long userId, Long postingId) {
		postingLookup.requireVisible(postingId);
		if (bookmarkRepository.existsByUserIdAndPostingId(userId, postingId)) {
			throw new BusinessException(PostingErrorCode.BOOKMARK_ALREADY_EXISTS);
		}
		LocalDateTime now = LocalDateTime.now();
		try {
			bookmarkRepository.saveAndFlush(PostingBookmarkRef.of(userId, postingId, now));
		} catch (DataIntegrityViolationException e) {
			throw new BusinessException(PostingErrorCode.BOOKMARK_ALREADY_EXISTS);
		}
		return new PostingBookmarkResponse(postingId, true, now);
	}

	/** 저장하지 않은 공고면 404. */
	public void remove(Long userId, Long postingId) {
		postingLookup.requireVisible(postingId);
		if (bookmarkRepository.deleteByUserIdAndPostingId(userId, postingId) == 0) {
			throw new BusinessException(PostingErrorCode.BOOKMARK_NOT_FOUND);
		}
	}
}

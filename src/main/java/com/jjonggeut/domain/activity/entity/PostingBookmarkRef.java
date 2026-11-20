package com.jjonggeut.domain.activity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * 사용자가 저장한 공고(posting_bookmark). 행은 추가·삭제만 하고 수정하지 않는다.
 */
@Entity
@Immutable
@Table(name = "posting_bookmark")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostingBookmarkRef {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "posting_bookmark_id")
	private Long postingBookmarkId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "posting_id", nullable = false)
	private Long postingId;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public static PostingBookmarkRef of(Long userId, Long postingId, LocalDateTime createdAt) {
		PostingBookmarkRef bookmark = new PostingBookmarkRef();
		bookmark.userId = userId;
		bookmark.postingId = postingId;
		bookmark.createdAt = createdAt;
		return bookmark;
	}
}

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
 * 사용자가 저장한 공고(posting_bookmark)의 읽기 전용 매핑. 북마크 추가·해제는 모집공고 API 의 몫이다.
 * NOTE: 실제 테이블이 확정되면 @Table/@Column 이름만 맞춘다. of() 는 테스트와 임시 데이터용이다.
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

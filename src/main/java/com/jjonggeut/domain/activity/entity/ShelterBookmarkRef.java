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

/** 사용자가 저장한 보호시설(shelter_bookmark)의 읽기 전용 매핑. 저장·해제는 보호시설 API 의 몫이다. */
@Entity
@Immutable
@Table(name = "shelter_bookmark")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShelterBookmarkRef {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "shelter_bookmark_id")
	private Long shelterBookmarkId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "shelter_id", nullable = false)
	private Long shelterId;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public static ShelterBookmarkRef of(Long userId, Long shelterId, LocalDateTime createdAt) {
		ShelterBookmarkRef bookmark = new ShelterBookmarkRef();
		bookmark.userId = userId;
		bookmark.shelterId = shelterId;
		bookmark.createdAt = createdAt;
		return bookmark;
	}
}

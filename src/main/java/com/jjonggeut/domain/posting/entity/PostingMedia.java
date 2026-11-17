package com.jjonggeut.domain.posting.entity;

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
import org.hibernate.annotations.CreationTimestamp;

/** 공고 사진. position 오름차순 1번이 대표(HERO) 사진이다. */
@Entity
@Table(name = "posting_media")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostingMedia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "posting_media_id")
	private Long postingMediaId;

	@Column(name = "posting_id", nullable = false)
	private Long postingId;

	@Column(name = "position", nullable = false)
	private Integer position;

	@Column(name = "role", length = 20)
	private String role;

	@Column(name = "url", nullable = false, length = 500)
	private String url;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public static PostingMedia of(Long postingId, Integer position, String role, String url) {
		PostingMedia media = new PostingMedia();
		media.postingId = postingId;
		media.position = position;
		media.role = role;
		media.url = url;
		return media;
	}
}

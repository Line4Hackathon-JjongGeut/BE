package com.jjonggeut.domain.recommendation.entity;

import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategoryListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * 추천이 읽는 공고(posting) 테이블의 읽기 전용 매핑.
 * NOTE: 공고 담당 팀의 실제 테이블이 확정되면 이 클래스의 @Table/@Column 이름만 맞춘다.
 * of() 는 테스트와 임시 데이터용이다.
 */
@Entity
@Immutable
@Table(name = "posting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostingRef {

	/** 추천 대상 공고 종류. */
	public static final String TYPE_VOLUNTEER = "VOLUNTEER";
	/** 모집 중 상태. */
	public static final String STATUS_OPEN = "OPEN";

	@Id
	@Column(name = "posting_id")
	private Long postingId;

	@Column(name = "shelter_id", nullable = false)
	private Long shelterId;

	@Column(name = "posting_type", nullable = false, length = 20)
	private String postingType;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "status", nullable = false, length = 20)
	private String status;

	@Convert(converter = ActivityCategoryListConverter.class)
	@Column(name = "activity_categories", nullable = false, length = 255)
	private List<ActivityCategory> activityCategories;

	/** DOG / CAT / ALL 등. 없으면 null. */
	@Column(name = "animal_type", length = 20)
	private String animalType;

	@Column(name = "activity_start_at")
	private LocalDateTime activityStartAt;

	@Column(name = "capacity")
	private Integer capacity;

	@Column(name = "applicant_count")
	private Integer applicantCount;

	public static PostingRef of(Long postingId, Long shelterId, String postingType, String title, String status,
			List<ActivityCategory> activityCategories, String animalType, LocalDateTime activityStartAt,
			Integer capacity, Integer applicantCount) {
		PostingRef posting = new PostingRef();
		posting.postingId = postingId;
		posting.shelterId = shelterId;
		posting.postingType = postingType;
		posting.title = title;
		posting.status = status;
		posting.activityCategories = activityCategories;
		posting.animalType = animalType;
		posting.activityStartAt = activityStartAt;
		posting.capacity = capacity;
		posting.applicantCount = applicantCount;
		return posting;
	}
}

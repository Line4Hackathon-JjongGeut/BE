package com.jjonggeut.domain.recommendation.entity;

import com.jjonggeut.domain.posting.entity.FosterContactMethod;
import com.jjonggeut.domain.posting.entity.PetGender;
import com.jjonggeut.domain.posting.entity.StringListConverter;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategoryListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

	// ---- 모집공고 API 가 쓰는 상세 정보. 값이 없는 옛 행도 읽을 수 있도록 모두 null 을 허용한다. ----

	@Column(name = "description", length = 2000)
	private String description;

	@Column(name = "urgent")
	private Boolean urgent;

	/** 클수록 먼저 보여 줄 긴급 우선순위. */
	@Column(name = "urgent_priority")
	private Integer urgentPriority;

	@Column(name = "recruit_end_at")
	private LocalDateTime recruitEndAt;

	@Column(name = "activity_end_at")
	private LocalDateTime activityEndAt;

	/** 경험 없이 참여할 수 있는지. 없으면 가능한 것으로 본다. */
	@Column(name = "beginner_friendly")
	private Boolean beginnerFriendly;

	@Column(name = "min_age")
	private Integer minAge;

	@Column(name = "requirements", length = 1000)
	private String requirements;

	/** 공개 여부. 없으면 공개로 본다. */
	@Column(name = "is_public")
	private Boolean publicPosting;

	@Column(name = "donation_url", length = 500)
	private String donationUrl;

	@Column(name = "donation_purpose", length = 255)
	private String donationPurpose;

	@Column(name = "donation_target_quantity")
	private Integer donationTargetQuantity;

	@Column(name = "donation_received_quantity")
	private Integer donationReceivedQuantity;

	@Enumerated(EnumType.STRING)
	@Column(name = "contact_method", length = 20)
	private FosterContactMethod contactMethod;

	@Column(name = "external_url", length = 500)
	private String externalUrl;

	@Column(name = "pet_name", length = 50)
	private String petName;

	@Column(name = "pet_age_text", length = 50)
	private String petAgeText;

	@Enumerated(EnumType.STRING)
	@Column(name = "pet_gender", length = 10)
	private PetGender petGender;

	@Column(name = "pet_characteristics", length = 500)
	private String petCharacteristics;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Convert(converter = StringListConverter.class)
	@Column(name = "preparations", length = 1000)
	private List<String> preparations;

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

	// ---- 테스트와 임시 데이터용: 저장하기 전에 상세 정보를 채운다. ----

	public PostingRef withDescription(String description, LocalDateTime createdAt) {
		this.description = description;
		this.createdAt = createdAt;
		return this;
	}

	public PostingRef withUrgent(boolean urgent, int urgentPriority) {
		this.urgent = urgent;
		this.urgentPriority = urgentPriority;
		return this;
	}

	public PostingRef withPeriod(LocalDateTime recruitEndAt, LocalDateTime activityEndAt) {
		this.recruitEndAt = recruitEndAt;
		this.activityEndAt = activityEndAt;
		return this;
	}

	public PostingRef withConditions(boolean beginnerFriendly, Integer minAge, String requirements) {
		this.beginnerFriendly = beginnerFriendly;
		this.minAge = minAge;
		this.requirements = requirements;
		return this;
	}

	public PostingRef withPublic(boolean publicPosting) {
		this.publicPosting = publicPosting;
		return this;
	}

	public PostingRef withSponsor(String donationUrl, String donationPurpose) {
		this.donationUrl = donationUrl;
		this.donationPurpose = donationPurpose;
		return this;
	}

	public PostingRef withDonationQuantity(Integer targetQuantity, Integer receivedQuantity) {
		this.donationTargetQuantity = targetQuantity;
		this.donationReceivedQuantity = receivedQuantity;
		return this;
	}

	public PostingRef withFoster(FosterContactMethod contactMethod, String externalUrl) {
		this.contactMethod = contactMethod;
		this.externalUrl = externalUrl;
		return this;
	}

	public PostingRef withPetInfo(String petName, String petAgeText, PetGender petGender,
			String petCharacteristics) {
		this.petName = petName;
		this.petAgeText = petAgeText;
		this.petGender = petGender;
		this.petCharacteristics = petCharacteristics;
		return this;
	}

	public PostingRef withPreparations(List<String> preparations) {
		this.preparations = preparations;
		return this;
	}

	public List<String> getPreparations() {
		return preparations == null ? List.of() : preparations;
	}

	public boolean isUrgent() {
		return Boolean.TRUE.equals(urgent);
	}

	public int getUrgentPriority() {
		return urgentPriority == null ? 0 : urgentPriority;
	}

	public boolean isBeginnerFriendly() {
		return beginnerFriendly == null || beginnerFriendly;
	}

	public boolean isPublicPosting() {
		return publicPosting == null || publicPosting;
	}
}

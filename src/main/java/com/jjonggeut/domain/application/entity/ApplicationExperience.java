package com.jjonggeut.domain.application.entity;

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

/** 신청에 첨부한 봉사 경험. 원본이 나중에 수정·삭제돼도 신청 당시 내용을 스냅샷으로 보존한다. */
@Entity
@Table(name = "application_experience")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationExperience {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "application_experience_id")
	private Long applicationExperienceId;

	@Column(name = "application_id", nullable = false)
	private Long applicationId;

	/** 원본 경험. 원본이 삭제되면 NULL 로 남는다. */
	@Column(name = "user_volunteer_experience_id")
	private Long experienceId;

	@Column(name = "experience_snapshot", nullable = false, columnDefinition = "TEXT")
	private String experienceSnapshot;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public static ApplicationExperience of(Long applicationId, Long experienceId, String experienceSnapshot) {
		ApplicationExperience entity = new ApplicationExperience();
		entity.applicationId = applicationId;
		entity.experienceId = experienceId;
		entity.experienceSnapshot = experienceSnapshot;
		return entity;
	}
}

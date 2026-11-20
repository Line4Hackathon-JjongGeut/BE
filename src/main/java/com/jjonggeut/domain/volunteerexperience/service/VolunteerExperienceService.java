package com.jjonggeut.domain.volunteerexperience.service;

import com.jjonggeut.domain.volunteerexperience.dto.ExperienceCreateRequest;
import com.jjonggeut.domain.volunteerexperience.dto.ExperienceListResponse;
import com.jjonggeut.domain.volunteerexperience.dto.ExperienceResponse;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import com.jjonggeut.domain.volunteerexperience.entity.VolunteerExperience;
import com.jjonggeut.domain.volunteerexperience.exception.ExperienceErrorCode;
import com.jjonggeut.domain.volunteerexperience.repository.VolunteerExperienceRepository;
import com.jjonggeut.global.exception.BusinessException;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VolunteerExperienceService {

	private final VolunteerExperienceRepository experienceRepository;

	public VolunteerExperienceService(VolunteerExperienceRepository experienceRepository) {
		this.experienceRepository = experienceRepository;
	}

	@Transactional
	public ExperienceResponse create(Long userId, ExperienceCreateRequest request) {
		// 중복 분류는 제거하고 입력 순서를 유지한다.
		List<ActivityCategory> categories = List.copyOf(new LinkedHashSet<>(request.activityCategories()));

		VolunteerExperience experience = VolunteerExperience.create(
				userId,
				categories,
				blankToNull(request.organizationName()),
				blankToNull(request.periodText()),
				request.content().trim(),
				blankToNull(request.summary()));
		return ExperienceResponse.from(experienceRepository.save(experience));
	}

	@Transactional(readOnly = true)
	public ExperienceListResponse list(Long userId) {
		List<ExperienceResponse> experiences = experienceRepository
				.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDescExperienceIdDesc(userId).stream()
				.map(ExperienceResponse::from)
				.toList();
		return new ExperienceListResponse(experiences);
	}

	/** 본인 항목만 삭제할 수 있다. 없거나 타인의 항목이거나 이미 삭제된 경우 모두 404 로 응답한다. */
	@Transactional
	public void delete(Long userId, Long experienceId) {
		VolunteerExperience experience = experienceRepository
				.findByExperienceIdAndUserIdAndDeletedAtIsNull(experienceId, userId)
				.orElseThrow(() -> new BusinessException(ExperienceErrorCode.EXPERIENCE_NOT_FOUND));
		experience.delete();
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}

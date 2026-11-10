package com.jjonggeut.domain.profile.service;

import com.jjonggeut.domain.profile.dto.ProfileResponse;
import com.jjonggeut.domain.profile.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

	private final UserProfileRepository profileRepository;

	public ProfileService(UserProfileRepository profileRepository) {
		this.profileRepository = profileRepository;
	}

	/** 프로필이 아직 없으면(온보딩 전) 404 가 아니라 빈 프로필로 응답한다. */
	@Transactional(readOnly = true)
	public ProfileResponse getMyProfile(Long userId) {
		return profileRepository.findByUserId(userId)
				.map(ProfileResponse::from)
				.orElseGet(ProfileResponse::empty);
	}
}

package com.jjonggeut.domain.user.service;

import com.jjonggeut.domain.auth.exception.AuthErrorCode;
import com.jjonggeut.domain.auth.repository.RefreshTokenRepository;
import com.jjonggeut.domain.profile.repository.UserProfileRepository;
import com.jjonggeut.domain.recommendation.repository.RecommendationBatchRepository;
import com.jjonggeut.domain.recommendation.repository.RecommendationItemRepository;
import com.jjonggeut.domain.savedsearch.repository.SavedSearchRepository;
import com.jjonggeut.domain.user.dto.UserResponse;
import com.jjonggeut.domain.user.dto.UserUpdateRequest;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.entity.UserStatus;
import com.jjonggeut.domain.user.repository.UserRepository;
import com.jjonggeut.domain.volunteerexperience.repository.VolunteerExperienceRepository;
import com.jjonggeut.global.exception.BusinessException;
import com.jjonggeut.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 내 계정 조회·수정·탈퇴. */
@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserProfileRepository profileRepository;
	private final VolunteerExperienceRepository experienceRepository;
	private final RecommendationItemRepository recommendationItemRepository;
	private final RecommendationBatchRepository recommendationBatchRepository;
	private final SavedSearchRepository savedSearchRepository;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			RefreshTokenRepository refreshTokenRepository, UserProfileRepository profileRepository,
			VolunteerExperienceRepository experienceRepository,
			RecommendationItemRepository recommendationItemRepository,
			RecommendationBatchRepository recommendationBatchRepository,
			SavedSearchRepository savedSearchRepository) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokenRepository = refreshTokenRepository;
		this.profileRepository = profileRepository;
		this.experienceRepository = experienceRepository;
		this.recommendationItemRepository = recommendationItemRepository;
		this.recommendationBatchRepository = recommendationBatchRepository;
		this.savedSearchRepository = savedSearchRepository;
	}

	@Transactional(readOnly = true)
	public UserResponse getMe(Long userId) {
		return UserResponse.from(findActiveUser(userId));
	}

	@Transactional
	public UserResponse updateMe(Long userId, UserUpdateRequest request) {
		User user = findActiveUser(userId);

		if (request.name() != null) {
			user.changeName(request.name());
		}
		if (request.nickname() != null && !request.nickname().equals(user.getNickname())) {
			if (userRepository.existsByNickname(request.nickname())) {
				throw new BusinessException(AuthErrorCode.NICKNAME_DUPLICATED);
			}
			user.changeNickname(request.nickname());
		}
		if (request.phone() != null) {
			user.changePhone(emptyToNull(request.phone()));
		}
		if (request.email() != null) {
			user.changeEmail(emptyToNull(request.email()));
		}

		try {
			// 동시 수정으로 사전 중복 검사를 통과해도 DB UNIQUE 제약에서 걸러진다.
			userRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException e) {
			throw new BusinessException(AuthErrorCode.NICKNAME_DUPLICATED);
		}
		return UserResponse.from(user);
	}

	/**
	 * 탈퇴 처리. 계정은 탈퇴 상태로 바꾸고 개인정보를 지우며, 로그인 세션·추천 프로필·봉사 경험·추천 기록·저장 검색 조건은 삭제한다.
	 * 동의 이력은 법적 기록이라 남기고, 행동 이벤트는 계정이 익명화되므로 그대로 둔다.
	 */
	@Transactional
	public void withdraw(Long userId) {
		User user = findActiveUser(userId);

		refreshTokenRepository.findByUserId(userId).ifPresent(refreshTokenRepository::delete);
		profileRepository.deleteByUserId(userId);
		experienceRepository.deleteByUserId(userId);
		recommendationItemRepository.deleteByUserId(userId);
		recommendationBatchRepository.deleteByUserId(userId);
		savedSearchRepository.deleteByUserId(userId);

		// 어떤 비밀번호로도 로그인할 수 없도록 아무도 모르는 값의 해시를 넣는다.
		user.withdraw(passwordEncoder.encode(UUID.randomUUID().toString()), LocalDateTime.now());
	}

	/** 토큰은 유효한데 회원이 없으면 401, 정상 상태가 아니면(탈퇴·정지) 403 으로 응답한다. */
	private User findActiveUser(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(AuthErrorCode.ACCOUNT_UNAVAILABLE);
		}
		return user;
	}

	private String emptyToNull(String value) {
		return value.isEmpty() ? null : value;
	}
}

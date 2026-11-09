package com.jjonggeut.domain.auth.service;

import com.jjonggeut.domain.auth.dto.LoginRequest;
import com.jjonggeut.domain.auth.dto.LoginResponse;
import com.jjonggeut.domain.auth.dto.SignupRequest;
import com.jjonggeut.domain.auth.dto.SignupResponse;
import com.jjonggeut.domain.auth.entity.RefreshToken;
import com.jjonggeut.domain.auth.exception.AuthErrorCode;
import com.jjonggeut.domain.auth.repository.RefreshTokenRepository;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.entity.UserStatus;
import com.jjonggeut.domain.user.repository.UserRepository;
import com.jjonggeut.global.exception.BusinessException;
import com.jjonggeut.global.jwt.JwtProvider;
import com.jjonggeut.global.util.HashUtil;
import java.time.LocalDateTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final String TOKEN_TYPE = "Bearer";

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;

	public AuthService(UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			JwtProvider jwtProvider) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtProvider = jwtProvider;
	}

	@Transactional
	public SignupResponse signup(SignupRequest request) {
		if (userRepository.existsByLoginId(request.loginId())) {
			throw new BusinessException(AuthErrorCode.LOGIN_ID_DUPLICATED);
		}
		if (userRepository.existsByNickname(request.nickname())) {
			throw new BusinessException(AuthErrorCode.NICKNAME_DUPLICATED);
		}

		User user = User.createUser(
				request.loginId(),
				passwordEncoder.encode(request.password()),
				request.name(),
				request.nickname());

		try {
			// 동시 가입으로 사전 중복 검사를 통과해도 DB UNIQUE 제약에서 걸러진다.
			userRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException e) {
			String cause = String.valueOf(e.getMostSpecificCause().getMessage()).toLowerCase();
			throw new BusinessException(cause.contains("nickname")
					? AuthErrorCode.NICKNAME_DUPLICATED
					: AuthErrorCode.LOGIN_ID_DUPLICATED);
		}

		return SignupResponse.from(user);
	}

	@Transactional
	public LoginResponse login(LoginRequest request) {
		// 아이디 없음과 비밀번호 불일치를 구분하지 않는다.
		User user = userRepository.findByLoginId(request.loginId())
				.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
		}
		// 비밀번호가 맞은 경우에만 계정 상태를 알려준다.
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(AuthErrorCode.ACCOUNT_UNAVAILABLE);
		}

		String accessToken = jwtProvider.createAccessToken(user.getUserId(), user.getRole().name());
		String refreshToken = jwtProvider.createRefreshToken(user.getUserId());
		saveRefreshToken(user.getUserId(), refreshToken);

		return new LoginResponse(
				user.getUserId(),
				user.getNickname(),
				accessToken,
				refreshToken,
				TOKEN_TYPE,
				jwtProvider.getAccessExpirationSeconds());
	}

	/** 리프레시 토큰을 삭제한다. 이미 로그아웃된 상태여도 성공으로 처리한다. */
	@Transactional
	public void logout(Long userId) {
		refreshTokenRepository.findByUserId(userId).ifPresent(refreshTokenRepository::delete);
	}

	/** 사용자당 1건만 유지한다. 재로그인하면 기존 리프레시 토큰은 무효가 된다. */
	private void saveRefreshToken(Long userId, String refreshToken) {
		String hash = HashUtil.sha256Hex(refreshToken);
		LocalDateTime expiresAt = LocalDateTime.now().plus(jwtProvider.getRefreshExpiration());

		refreshTokenRepository.findByUserId(userId).ifPresentOrElse(
				existing -> existing.rotate(hash, expiresAt),
				() -> refreshTokenRepository.save(RefreshToken.create(userId, hash, expiresAt)));
	}
}

package com.jjonggeut.domain.auth.service;

import com.jjonggeut.domain.auth.dto.LoginRequest;
import com.jjonggeut.domain.auth.dto.LoginResponse;
import com.jjonggeut.domain.auth.dto.PasswordChangeRequest;
import com.jjonggeut.domain.auth.dto.PasswordResetRequest;
import com.jjonggeut.domain.auth.dto.PasswordResetResponse;
import com.jjonggeut.domain.auth.dto.SignupRequest;
import com.jjonggeut.domain.auth.dto.TokenRefreshRequest;
import com.jjonggeut.domain.auth.dto.TokenRefreshResponse;
import com.jjonggeut.domain.auth.dto.SignupResponse;
import com.jjonggeut.domain.auth.entity.RefreshToken;
import com.jjonggeut.domain.auth.exception.AuthErrorCode;
import com.jjonggeut.domain.auth.repository.RefreshTokenRepository;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.entity.UserStatus;
import com.jjonggeut.domain.user.repository.UserRepository;
import com.jjonggeut.global.exception.BusinessException;
import com.jjonggeut.global.exception.ErrorCode;
import com.jjonggeut.global.jwt.JwtProvider;
import com.jjonggeut.global.util.HashUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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

	/** 저장된 리프레시 토큰과 일치할 때만 액세스 토큰을 재발급한다. 리프레시 토큰은 그대로 유지한다. */
	@Transactional(readOnly = true)
	public TokenRefreshResponse refreshAccessToken(TokenRefreshRequest request) {
		String token = request.refreshToken();

		Claims claims;
		try {
			claims = jwtProvider.parseRefreshToken(token);
		} catch (JwtException | IllegalArgumentException e) {
			throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}

		// 로그아웃했거나 재로그인으로 교체된 토큰은 DB 에 없다. 원인은 구분하지 않는다.
		RefreshToken stored = refreshTokenRepository.findByTokenHash(HashUtil.sha256Hex(token))
				.filter(saved -> saved.getExpiresAt().isAfter(LocalDateTime.now()))
				.filter(saved -> String.valueOf(saved.getUserId()).equals(claims.getSubject()))
				.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));

		User user = userRepository.findById(stored.getUserId())
				.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(AuthErrorCode.ACCOUNT_UNAVAILABLE);
		}

		return new TokenRefreshResponse(
				jwtProvider.createAccessToken(user.getUserId(), user.getRole().name()),
				TOKEN_TYPE,
				jwtProvider.getAccessExpirationSeconds());
	}

	/**
	 * 이름과 아이디로 본인을 확인하고 비밀번호 재설정용 임시 토큰을 발급한다.
	 * 이메일 인증이 없는 시연용 방식이다.
	 */
	@Transactional(readOnly = true)
	public PasswordResetResponse requestPasswordReset(PasswordResetRequest request) {
		User user = userRepository.findByLoginId(request.loginId())
				.filter(found -> found.getName().equals(request.name().trim()))
				.orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(AuthErrorCode.ACCOUNT_UNAVAILABLE);
		}

		return new PasswordResetResponse(
				jwtProvider.createPasswordResetToken(user.getUserId(), passwordFingerprint(user)),
				jwtProvider.getPasswordResetExpirationSeconds());
	}

	/**
	 * 비밀번호를 변경하고 기존 세션(리프레시 토큰)을 정리한다.
	 * 로그인 상태(authenticatedUserId)이면 현재 비밀번호로 확인하고,
	 * 아니면 비밀번호 재설정 요청으로 받은 재설정 토큰으로 확인한다.
	 */
	@Transactional
	public void changePassword(Long authenticatedUserId, PasswordChangeRequest request) {
		User user;
		if (authenticatedUserId != null) {
			user = userRepository.findById(authenticatedUserId)
					.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
			verifyActive(user);
			if (request.currentPassword() == null
					|| !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
				throw new BusinessException(AuthErrorCode.CURRENT_PASSWORD_MISMATCH);
			}
		} else if (request.resetToken() != null && !request.resetToken().isBlank()) {
			user = verifyResetToken(request.resetToken());
			verifyActive(user);
		} else {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
			throw new BusinessException(AuthErrorCode.SAME_PASSWORD);
		}

		user.changePassword(passwordEncoder.encode(request.newPassword()));
		// 다른 기기의 로그인 세션을 종료한다.
		refreshTokenRepository.findByUserId(user.getUserId()).ifPresent(refreshTokenRepository::delete);
	}

	/** 서명·만료·종류를 확인하고, 발급 이후 비밀번호가 바뀌지 않았는지(1회용) 확인한다. */
	private User verifyResetToken(String resetToken) {
		Claims claims;
		try {
			claims = jwtProvider.parsePasswordResetToken(resetToken);
		} catch (JwtException | IllegalArgumentException e) {
			throw new BusinessException(AuthErrorCode.INVALID_RESET_TOKEN);
		}

		User user = userRepository.findById(Long.valueOf(claims.getSubject()))
				.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_RESET_TOKEN));
		if (!passwordFingerprint(user).equals(claims.get("pwd", String.class))) {
			throw new BusinessException(AuthErrorCode.INVALID_RESET_TOKEN);
		}
		return user;
	}

	private void verifyActive(User user) {
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(AuthErrorCode.ACCOUNT_UNAVAILABLE);
		}
	}

	private String passwordFingerprint(User user) {
		return HashUtil.sha256Hex(user.getPasswordHash()).substring(0, 16);
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

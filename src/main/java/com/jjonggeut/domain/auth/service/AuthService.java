package com.jjonggeut.domain.auth.service;

import com.jjonggeut.domain.auth.dto.SignupRequest;
import com.jjonggeut.domain.auth.dto.SignupResponse;
import com.jjonggeut.domain.auth.exception.AuthErrorCode;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.repository.UserRepository;
import com.jjonggeut.global.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
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
		userRepository.save(user);

		return SignupResponse.from(user);
	}
}

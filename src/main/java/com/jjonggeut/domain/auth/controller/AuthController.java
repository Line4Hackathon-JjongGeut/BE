package com.jjonggeut.domain.auth.controller;

import com.jjonggeut.domain.auth.dto.LoginRequest;
import com.jjonggeut.domain.auth.dto.LoginResponse;
import com.jjonggeut.domain.auth.dto.PasswordResetRequest;
import com.jjonggeut.domain.auth.dto.PasswordResetResponse;
import com.jjonggeut.domain.auth.dto.SignupRequest;
import com.jjonggeut.domain.auth.dto.SignupResponse;
import com.jjonggeut.domain.auth.dto.TokenRefreshRequest;
import com.jjonggeut.domain.auth.dto.TokenRefreshResponse;
import com.jjonggeut.domain.auth.service.AuthService;
import com.jjonggeut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@Operation(summary = "회원가입", description = "일반 사용자 계정을 생성한다.")
	@PostMapping("/signup")
	public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
		return ApiResponse.success(authService.signup(request));
	}

	@Operation(summary = "로그인", description = "아이디와 비밀번호로 로그인하고 액세스·리프레시 토큰을 발급한다.")
	@PostMapping("/login")
	public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.success(authService.login(request));
	}

	@Operation(summary = "로그아웃", description = "현재 리프레시 토큰과 로그인 세션을 만료시킨다.",
			security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping("/logout")
	public ApiResponse<Void> logout(@AuthenticationPrincipal Long userId) {
		authService.logout(userId);
		return ApiResponse.success();
	}

	@Operation(summary = "액세스 토큰 재발급", description = "유효한 리프레시 토큰으로 액세스 토큰을 재발급한다.")
	@PostMapping("/tokens/refresh")
	public ApiResponse<TokenRefreshResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
		return ApiResponse.success(authService.refreshAccessToken(request));
	}

	@Operation(summary = "비밀번호 재설정 요청", description = "이름과 아이디로 본인을 확인하고, 비밀번호를 재설정할 수 있는 임시 토큰을 발급한다.")
	@PostMapping("/password/reset-requests")
	public ApiResponse<PasswordResetResponse> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
		return ApiResponse.success(authService.requestPasswordReset(request));
	}
}

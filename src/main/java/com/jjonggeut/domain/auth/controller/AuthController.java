package com.jjonggeut.domain.auth.controller;

import com.jjonggeut.domain.auth.dto.SignupRequest;
import com.jjonggeut.domain.auth.dto.SignupResponse;
import com.jjonggeut.domain.auth.service.AuthService;
import com.jjonggeut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
}

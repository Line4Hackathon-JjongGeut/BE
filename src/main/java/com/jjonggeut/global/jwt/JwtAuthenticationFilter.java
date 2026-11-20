package com.jjonggeut.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization 헤더의 액세스 토큰을 검증해 인증 정보를 등록한다.
 * 토큰이 없거나 유효하지 않으면 인증 없이 통과시키고, 인증이 필요한 경로는 EntryPoint 가 401 로 응답한다.
 * Spring Bean 으로 등록하면 서블릿 필터로 중복 등록되므로 SecurityConfig 에서 직접 생성한다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtProvider jwtProvider;

	public JwtAuthenticationFilter(JwtProvider jwtProvider) {
		this.jwtProvider = jwtProvider;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			String token = header.substring(BEARER_PREFIX.length()).trim();
			try {
				Claims claims = jwtProvider.parseAccessToken(token);
				Long userId = Long.valueOf(claims.getSubject());
				String role = claims.get("role", String.class);

				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (JwtException | IllegalArgumentException e) {
				SecurityContextHolder.clearContext();
			}
		}
		chain.doFilter(request, response);
	}
}

package com.jjonggeut.global.config;

import com.jjonggeut.global.jwt.JwtAccessDeniedHandler;
import com.jjonggeut.global.jwt.JwtAuthenticationEntryPoint;
import com.jjonggeut.global.jwt.JwtAuthenticationFilter;
import com.jjonggeut.global.jwt.JwtProvider;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private static final String[] PUBLIC_PATHS = {
			"/api/v1/auth/signup",
			"/api/v1/auth/login",
			"/api/v1/auth/login-id-availability",
			"/api/v1/auth/nickname-availability",
			"/media/**",
			"/api/v1/auth/tokens/refresh",
			"/api/v1/auth/password/reset-requests",
			"/swagger-ui.html",
			"/swagger-ui/**",
			"/v3/api-docs/**",
			"/actuator/health/**"
	};

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			JwtProvider jwtProvider,
			JwtAuthenticationEntryPoint authenticationEntryPoint,
			JwtAccessDeniedHandler accessDeniedHandler) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(PUBLIC_PATHS).permitAll()
						// 로그인 상태(액세스 토큰) 또는 재설정 토큰 중 하나로 호출하므로, 인증 판단은 서비스에서 한다.
						.requestMatchers(HttpMethod.PATCH, "/api/v1/auth/password").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(
			@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(allowedOrigins);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(false);
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}

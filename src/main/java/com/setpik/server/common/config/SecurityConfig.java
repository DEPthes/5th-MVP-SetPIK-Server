package com.setpik.server.common.config;

import com.setpik.server.common.security.RestAccessDeniedHandler;
import com.setpik.server.common.security.RestAuthenticationEntryPoint;
import com.setpik.server.common.security.ApiBearerTokenResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		RestAuthenticationEntryPoint authenticationEntryPoint,
		RestAccessDeniedHandler accessDeniedHandler,
		ApiBearerTokenResolver bearerTokenResolver
	) throws Exception {
		return http
			// REST API는 세션 대신 추후 추가할 JWT 필터로 인증한다.
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			// Spring Security 표준 필터가 Bearer JWT의 서명과 만료시간을 검증한다.
			.oauth2ResourceServer(oauth2 -> oauth2
				.jwt(Customizer.withDefaults())
				.bearerTokenResolver(bearerTokenResolver)
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/api/v1/health",
					"/actuator/health/**",
					"/api/v1/auth/**",
					"/swagger-ui/**",
					"/v3/api-docs/**",
					"/error"
				).permitAll()
				// API 명세서에서 공연 상세와 티켓 일정은 비회원도 조회할 수 있다.
				.requestMatchers(HttpMethod.GET,
					"/api/v1/performances/*",
					"/api/v1/performances/*/ticket-schedules"
				).permitAll()
				.anyRequest().authenticated())
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler))
			.build();
	}
}

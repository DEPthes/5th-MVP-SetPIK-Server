package com.setpik.server.common.config;

import com.setpik.server.common.security.RestAccessDeniedHandler;
import com.setpik.server.common.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
		RestAccessDeniedHandler accessDeniedHandler
	) throws Exception {
		return http
			// REST API는 세션 대신 추후 추가할 JWT 필터로 인증한다.
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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

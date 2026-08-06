package com.setpik.server.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.setpik.server.auth.domain.AuthRefreshToken;
import com.setpik.server.auth.repository.AuthRefreshTokenRepository;
import com.setpik.server.auth.security.TokenHasher;
import com.setpik.server.member.domain.User;
import com.setpik.server.member.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthLogoutControllerIntegrationTest {

	private static final String LOGOUT_URL = "/api/v1/auth/logout";
	private static final String REFRESH_COOKIE_NAME = "refreshToken";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AuthRefreshTokenRepository refreshTokenRepository;

	@Autowired
	private TokenHasher tokenHasher;

	@Test
	void revokesRefreshTokenAndDeletesCookie() throws Exception {
		String rawRefreshToken = "logout-refresh-token";
		AuthRefreshToken storedToken = saveRefreshToken(rawRefreshToken, LocalDateTime.now().plusDays(1));

		mockMvc.perform(post(LOGOUT_URL)
			.header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-invalid-access-token")
			.cookie(new Cookie(REFRESH_COOKIE_NAME, rawRefreshToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.message").value("로그아웃되었습니다."))
			.andExpect(jsonPath("$.result").value(nullValue()))
			.andExpect(header().string(HttpHeaders.SET_COOKIE,
				org.hamcrest.Matchers.allOf(
					org.hamcrest.Matchers.containsString("refreshToken="),
					org.hamcrest.Matchers.containsString("Max-Age=0"),
					org.hamcrest.Matchers.containsString("Path=/api/v1/auth"),
					org.hamcrest.Matchers.containsString("HttpOnly")
				)));

		assertThat(refreshTokenRepository.findById(storedToken.getRefreshTokenId()).orElseThrow()
			.getRevokedAt()).isNotNull();
	}

	@Test
	void returns400AndDeletesCookieWhenRefreshCookieIsMissing() throws Exception {
		mockMvc.perform(post(LOGOUT_URL))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value(2000))
			.andExpect(header().string(HttpHeaders.SET_COOKIE,
				org.hamcrest.Matchers.containsString("Max-Age=0")));
	}

	@Test
	void returns400WhenRefreshTokenIsUnknown() throws Exception {
		mockMvc.perform(post(LOGOUT_URL)
			.cookie(new Cookie(REFRESH_COOKIE_NAME, "unknown-refresh-token")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));
	}

	@Test
	void returns409AndDeletesCookieWhenRefreshTokenIsAlreadyRevoked() throws Exception {
		String rawRefreshToken = "revoked-refresh-token";
		AuthRefreshToken storedToken = saveRefreshToken(rawRefreshToken, LocalDateTime.now().plusDays(1));
		storedToken.revoke(LocalDateTime.now());
		refreshTokenRepository.flush();

		mockMvc.perform(post(LOGOUT_URL)
			.cookie(new Cookie(REFRESH_COOKIE_NAME, rawRefreshToken)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value(2004))
			.andExpect(header().string(HttpHeaders.SET_COOKIE,
				org.hamcrest.Matchers.containsString("Max-Age=0")));
	}

	private AuthRefreshToken saveRefreshToken(String rawRefreshToken, LocalDateTime expiresAt) {
		User user = userRepository.saveAndFlush(User.createActive(LocalDateTime.now()));
		return refreshTokenRepository.saveAndFlush(AuthRefreshToken.issue(
			tokenHasher.hash(rawRefreshToken),
			expiresAt,
			user.getUserId()
		));
	}
}

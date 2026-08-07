package com.setpik.server.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthTokenControllerIntegrationTest {

	private static final String REFRESH_URL = "/api/v1/auth/token/refresh";
	private static final String REFRESH_COOKIE_NAME = "refreshToken";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AuthRefreshTokenRepository refreshTokenRepository;

	@Autowired
	private TokenHasher tokenHasher;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void reissuesAccessTokenFromValidRefreshToken() throws Exception {
		User user = userRepository.saveAndFlush(User.createActive(LocalDateTime.now()));
		String rawRefreshToken = "valid-refresh-token";
		AuthRefreshToken storedToken = refreshTokenRepository.saveAndFlush(AuthRefreshToken.issue(
			tokenHasher.hash(rawRefreshToken),
			LocalDateTime.now().plusDays(1),
			user.getUserId()
		));

		MvcResult result = mockMvc.perform(post(REFRESH_URL)
			.cookie(new Cookie(REFRESH_COOKIE_NAME, rawRefreshToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.message").value("Access Token이 재발급되었습니다."))
			.andExpect(jsonPath("$.result.accessToken").isNotEmpty())
			.andReturn();

		JsonNode responseBody = objectMapper.readTree(result.getResponse().getContentAsByteArray());
		String accessToken = responseBody.path("result").path("accessToken").asText();
		var decodedJwt = jwtDecoder.decode(accessToken);
		assertThat(decodedJwt.getSubject()).isEqualTo(user.getUserId().toString());
		assertThat(decodedJwt.getClaimAsString("tokenType")).isEqualTo("access");
		assertThat(refreshTokenRepository.findById(storedToken.getRefreshTokenId()).orElseThrow()
			.getLastUsedAt()).isNotNull();
	}

	@Test
	void returns401WhenRefreshTokenCookieIsMissing() throws Exception {
		mockMvc.perform(post(REFRESH_URL))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value(2001));
	}

	@Test
	void returns401WhenRefreshTokenIsExpired() throws Exception {
		User user = userRepository.saveAndFlush(User.createActive(LocalDateTime.now()));
		String rawRefreshToken = "expired-refresh-token";
		refreshTokenRepository.saveAndFlush(AuthRefreshToken.issue(
			tokenHasher.hash(rawRefreshToken),
			LocalDateTime.now().minusMinutes(1),
			user.getUserId()
		));

		mockMvc.perform(post(REFRESH_URL)
			.cookie(new Cookie(REFRESH_COOKIE_NAME, rawRefreshToken)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(2001));
	}

	@Test
	void returns404WhenRefreshTokenUserDoesNotExist() throws Exception {
		String rawRefreshToken = "orphan-refresh-token";
		refreshTokenRepository.saveAndFlush(AuthRefreshToken.issue(
			tokenHasher.hash(rawRefreshToken),
			LocalDateTime.now().plusDays(1),
			999_999L
		));

		mockMvc.perform(post(REFRESH_URL)
			.cookie(new Cookie(REFRESH_COOKIE_NAME, rawRefreshToken)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value(2003));
	}
}

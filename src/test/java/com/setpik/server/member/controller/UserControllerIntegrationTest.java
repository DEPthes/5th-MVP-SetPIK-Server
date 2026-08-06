package com.setpik.server.member.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.setpik.server.auth.security.JwtAccessTokenProvider;
import com.setpik.server.member.domain.User;
import com.setpik.server.member.repository.UserRepository;
import com.setpik.server.spotify.domain.SpotifyAccount;
import com.setpik.server.spotify.repository.SpotifyAccountRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest {

	private static final String PROFILE_URL = "/api/v1/users/me";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SpotifyAccountRepository spotifyAccountRepository;

	@Autowired
	private JwtAccessTokenProvider accessTokenProvider;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Test
	void returnsAuthenticatedUserAndSpotifyProfile() throws Exception {
		LocalDateTime loginAt = LocalDateTime.of(2026, 7, 28, 9, 10, 11);
		User user = userRepository.saveAndFlush(User.createActive(loginAt));
		spotifyAccountRepository.saveAndFlush(SpotifyAccount.connect(
			"31abcde",
			"user@example.com",
			"setpik_user",
			"https://i.scdn.co/image/abc123",
			"encrypted-access-token",
			"encrypted-refresh-token",
			loginAt.plusHours(1),
			user.getUserId(),
			loginAt
		));
		String accessToken = accessTokenProvider.issue(user.getUserId());

		mockMvc.perform(get(PROFILE_URL)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
			.andExpect(jsonPath("$.result.userId").value(user.getUserId()))
			.andExpect(jsonPath("$.result.status").value("ACTIVE"))
			.andExpect(jsonPath("$.result.lastLoginAt").value("2026-07-28T09:10:11+09:00"))
			.andExpect(jsonPath("$.result.spotifyConnected").value(true))
			.andExpect(jsonPath("$.result.spotifyAccount.spotifyUserId").value("31abcde"))
			.andExpect(jsonPath("$.result.spotifyAccount.displayName").value("setpik_user"))
			.andExpect(jsonPath("$.result.spotifyAccount.profileImageUrl")
				.value("https://i.scdn.co/image/abc123"));
	}

	@Test
	void returnsDisconnectedProfileWhenSpotifyAccountDoesNotExist() throws Exception {
		User user = userRepository.saveAndFlush(User.createActive(LocalDateTime.now()));
		String accessToken = accessTokenProvider.issue(user.getUserId());

		mockMvc.perform(get(PROFILE_URL)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.spotifyConnected").value(false))
			.andExpect(jsonPath("$.result.spotifyAccount").doesNotExist());
	}

	@Test
	void returns401WhenBearerTokenIsMissing() throws Exception {
		mockMvc.perform(get(PROFILE_URL))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value(2001));
	}

	@Test
	void returns401WhenBearerTokenIsInvalid() throws Exception {
		mockMvc.perform(get(PROFILE_URL)
			.header(HttpHeaders.AUTHORIZATION, "Bearer forged-token"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(2001));
	}

	@Test
	void returns400WhenJwtSubjectIsNotUserId() throws Exception {
		String accessToken = issueAccessTokenWithSubject("not-a-number");

		mockMvc.perform(get(PROFILE_URL)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value(2000));
	}

	private String issueAccessTokenWithSubject(String subject) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer("setpik-server")
			.subject(subject)
			.issuedAt(now)
			.expiresAt(now.plusSeconds(600))
			.claim("tokenType", "access")
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
			.type("JWT")
			.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}

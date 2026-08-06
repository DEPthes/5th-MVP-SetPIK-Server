package com.setpik.server.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.setpik.server.auth.client.SpotifyApiException;
import com.setpik.server.auth.client.SpotifyOAuthClient;
import com.setpik.server.auth.client.dto.SpotifyProfileResponse;
import com.setpik.server.auth.client.dto.SpotifyProfileResponse.SpotifyImageResponse;
import com.setpik.server.auth.client.dto.SpotifyTokenResponse;
import com.setpik.server.auth.repository.AuthRefreshTokenRepository;
import com.setpik.server.member.domain.UserStatus;
import com.setpik.server.member.repository.UserRepository;
import com.setpik.server.spotify.domain.ConnectionStatus;
import com.setpik.server.spotify.repository.SpotifyAccountRepository;
import com.setpik.server.spotify.repository.SpotifyAccountScopeRepository;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SpotifyOAuthCallbackIntegrationTest {

	private static final String CALLBACK_URL = "/api/v1/auth/spotify/callback";
	private static final String STATE_COOKIE_NAME = "setpik_spotify_oauth_state";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SpotifyAccountRepository spotifyAccountRepository;

	@Autowired
	private SpotifyAccountScopeRepository spotifyAccountScopeRepository;

	@Autowired
	private AuthRefreshTokenRepository authRefreshTokenRepository;

	@MockitoBean
	private SpotifyOAuthClient spotifyOAuthClient;

	@Test
	void createsLoginDataAndRedirectsToSuccessPage() throws Exception {
		String state = "valid-oauth-state";
		stubSpotifyLogin("spotify-user-1", "setpik-user", "spotify-access-token", "spotify-refresh-token");

		MvcResult result = performCallback("authorization-code", state, state)
			.andExpect(status().isFound())
			.andExpect(header().string(HttpHeaders.LOCATION, "http://localhost:3000/oauth/success"))
			.andReturn();

		assertThat(userRepository.count()).isEqualTo(1);
		assertThat(spotifyAccountRepository.count()).isEqualTo(1);
		assertThat(spotifyAccountScopeRepository.count()).isEqualTo(2);
		assertThat(authRefreshTokenRepository.count()).isEqualTo(1);

		var user = userRepository.findAll().get(0);
		var account = spotifyAccountRepository.findAll().get(0);
		var refreshToken = authRefreshTokenRepository.findAll().get(0);
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(user.getLastLoginAt()).isNotNull();
		assertThat(account.getConnectionStatus()).isEqualTo(ConnectionStatus.CONNECTED);
		assertThat(account.getAccessTokenEncrypted()).isNotEqualTo("spotify-access-token");
		assertThat(account.getRefreshTokenEncrypted()).isNotEqualTo("spotify-refresh-token");
		assertThat(refreshToken.getTokenHash()).hasSize(64);

		List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
		assertThat(setCookies).anyMatch(cookie -> cookie.startsWith("refreshToken=") && cookie.contains("HttpOnly"));
		assertThat(setCookies).anyMatch(cookie -> cookie.startsWith(STATE_COOKIE_NAME + "=") && cookie.contains("Max-Age=0"));
	}

	@Test
	void updatesExistingAccountWithoutCreatingDuplicateUser() throws Exception {
		String state = "valid-oauth-state";
		stubSpotifyLogin("spotify-user-1", "first-name", "first-access-token", "first-refresh-token");
		performCallback("first-code", state, state).andExpect(status().isFound());

		stubSpotifyLogin("spotify-user-1", "updated-name", "updated-access-token", "updated-refresh-token");
		performCallback("second-code", state, state).andExpect(status().isFound());

		assertThat(userRepository.count()).isEqualTo(1);
		assertThat(spotifyAccountRepository.count()).isEqualTo(1);
		assertThat(authRefreshTokenRepository.count()).isEqualTo(2);
		assertThat(spotifyAccountRepository.findAll().get(0).getDisplayName()).isEqualTo("updated-name");
	}

	@Test
	void redirectsToCode2000WhenStateDoesNotMatch() throws Exception {
		performCallback("authorization-code", "returned-state", "stored-state")
			.andExpect(status().isFound())
			.andExpect(header().string(
				HttpHeaders.LOCATION,
				"http://localhost:3000/oauth/failure?code=2000"
			));

		verifyNoInteractions(spotifyOAuthClient);
	}

	@Test
	void redirectsToCode2200WhenSpotifyTokenExchangeFails() throws Exception {
		String state = "valid-oauth-state";
		when(spotifyOAuthClient.exchangeCode(anyString()))
			.thenThrow(new SpotifyApiException("Spotify token error"));

		performCallback("invalid-code", state, state)
			.andExpect(status().isFound())
			.andExpect(header().string(
				HttpHeaders.LOCATION,
				"http://localhost:3000/oauth/failure?code=2200"
			));
	}

	private org.springframework.test.web.servlet.ResultActions performCallback(
		String code,
		String state,
		String storedState
	) throws Exception {
		return mockMvc.perform(get(CALLBACK_URL)
			.param("code", code)
			.param("state", state)
			.cookie(new Cookie(STATE_COOKIE_NAME, storedState)));
	}

	private void stubSpotifyLogin(
		String spotifyUserId,
		String displayName,
		String accessToken,
		String refreshToken
	) {
		SpotifyTokenResponse tokenResponse = new SpotifyTokenResponse(
			accessToken,
			"Bearer",
			"user-read-email playlist-read-private",
			3600,
			refreshToken
		);
		SpotifyProfileResponse profileResponse = new SpotifyProfileResponse(
			spotifyUserId,
			null,
			"user@example.com",
			displayName,
			List.of(new SpotifyImageResponse("https://image.example/profile.jpg"))
		);
		when(spotifyOAuthClient.exchangeCode(anyString())).thenReturn(tokenResponse);
		when(spotifyOAuthClient.getCurrentUser(accessToken)).thenReturn(profileResponse);
	}
}

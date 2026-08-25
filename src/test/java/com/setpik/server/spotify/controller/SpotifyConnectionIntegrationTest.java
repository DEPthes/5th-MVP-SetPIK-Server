package com.setpik.server.spotify.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.setpik.server.auth.security.JwtAccessTokenProvider;
import com.setpik.server.member.domain.User;
import com.setpik.server.member.repository.UserRepository;
import com.setpik.server.spotify.domain.ConnectionStatus;
import com.setpik.server.spotify.domain.SpotifyAccount;
import com.setpik.server.spotify.domain.SpotifyAccountScope;
import com.setpik.server.spotify.repository.SpotifyAccountRepository;
import com.setpik.server.spotify.repository.SpotifyAccountScopeRepository;
import java.time.LocalDateTime;
import java.util.List;
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
class SpotifyConnectionIntegrationTest {

	private static final String CONNECTION_URL = "/api/v1/users/me/spotify-connection";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SpotifyAccountRepository spotifyAccountRepository;

	@Autowired
	private SpotifyAccountScopeRepository spotifyAccountScopeRepository;

	@Autowired
	private JwtAccessTokenProvider accessTokenProvider;

	@Test
	void returnsSpotifyConnectionAndGrantedScopes() throws Exception {
		LocalDateTime connectedAt = LocalDateTime.of(2026, 7, 28, 10, 0);
		LocalDateTime tokenExpiresAt = LocalDateTime.of(2026, 7, 28, 11, 0);
		User user = userRepository.saveAndFlush(User.createActive(connectedAt));
		SpotifyAccount account = spotifyAccountRepository.saveAndFlush(SpotifyAccount.connect(
			"31abcde",
			"user@example.com",
			"setpik_user",
			null,
			"encrypted-access-token",
			"encrypted-refresh-token",
			tokenExpiresAt,
			user.getUserId(),
			connectedAt
		));
		spotifyAccountScopeRepository.saveAllAndFlush(List.of(
			SpotifyAccountScope.grant("user-read-email", account.getSpotifyAccountId(), connectedAt),
			SpotifyAccountScope.grant("playlist-read-private", account.getSpotifyAccountId(), connectedAt)
		));

		mockMvc.perform(get(CONNECTION_URL)
			.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
			.andExpect(jsonPath("$.result.connected").value(true))
			.andExpect(jsonPath("$.result.connectionStatus").value("CONNECTED"))
			.andExpect(jsonPath("$.result.tokenExpiresAt").value("2026-07-28T11:00:00+09:00"))
			.andExpect(jsonPath("$.result.scopes.length()").value(2))
			.andExpect(jsonPath("$.result.scopes[0].scopeName").value("playlist-read-private"))
			.andExpect(jsonPath("$.result.scopes[0].isGranted").value(true))
			.andExpect(jsonPath("$.result.scopes[1].scopeName").value("user-read-email"))
			.andExpect(jsonPath("$.result.scopes[1].isGranted").value(true));
	}

	@Test
	void returnsDisconnectedWhenSpotifyAccountDoesNotExist() throws Exception {
		User user = userRepository.saveAndFlush(User.createActive(LocalDateTime.now()));

		mockMvc.perform(get(CONNECTION_URL)
			.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.connected").value(false))
			.andExpect(jsonPath("$.result.connectionStatus").value("DISCONNECTED"))
			.andExpect(jsonPath("$.result.tokenExpiresAt").doesNotExist())
			.andExpect(jsonPath("$.result.scopes").isEmpty());
	}

	@Test
	void returns401WhenBearerTokenIsMissing() throws Exception {
		mockMvc.perform(get(CONNECTION_URL))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value(2001));
	}

	@Test
	void disconnectsSpotifyAndClearsStoredTokensAndGrantedScopes() throws Exception {
		LocalDateTime connectedAt = LocalDateTime.of(2026, 8, 25, 10, 0);
		User user = userRepository.saveAndFlush(User.createActive(connectedAt));
		SpotifyAccount account = spotifyAccountRepository.saveAndFlush(SpotifyAccount.connect(
			"spotify-disconnect-user",
			"disconnect@example.com",
			"disconnect-user",
			null,
			"encrypted-access-token",
			"encrypted-refresh-token",
			connectedAt.plusHours(1),
			user.getUserId(),
			connectedAt
		));
		spotifyAccountScopeRepository.saveAndFlush(SpotifyAccountScope.grant(
			"playlist-read-private", account.getSpotifyAccountId(), connectedAt));
		String authorization = bearerToken(user.getUserId());

		mockMvc.perform(delete(CONNECTION_URL)
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.message").value("Spotify 연동이 해제되었습니다."))
			.andExpect(jsonPath("$.result").doesNotExist());

		SpotifyAccount disconnected = spotifyAccountRepository.findByUserId(user.getUserId())
			.orElseThrow();
		assertThat(disconnected.getConnectionStatus()).isEqualTo(ConnectionStatus.DISCONNECTED);
		assertThat(disconnected.getAccessTokenEncrypted()).isNull();
		assertThat(disconnected.getRefreshTokenEncrypted()).isNull();
		assertThat(disconnected.getTokenExpiresAt()).isNull();
		assertThat(disconnected.getDisconnectedAt()).isNotNull();
		SpotifyAccountScope revokedScope = spotifyAccountScopeRepository
			.findAllBySpotifyAccountIdOrderByScopeNameAsc(account.getSpotifyAccountId()).get(0);
		assertThat(revokedScope.getIsGranted()).isFalse();
		assertThat(revokedScope.getRevokedAt()).isNotNull();

		mockMvc.perform(get(CONNECTION_URL)
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.connected").value(false))
			.andExpect(jsonPath("$.result.connectionStatus").value("DISCONNECTED"))
			.andExpect(jsonPath("$.result.tokenExpiresAt").doesNotExist())
			.andExpect(jsonPath("$.result.scopes").isEmpty());
	}

	private String bearerToken(Long userId) {
		return "Bearer " + accessTokenProvider.issue(userId);
	}
}

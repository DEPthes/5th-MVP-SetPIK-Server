package com.setpik.server.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.setpik.server.auth.config.SpotifyOAuthProperties;
import com.setpik.server.auth.dto.SpotifyLoginUrlResponse;
import com.setpik.server.auth.support.OAuthStateGenerator;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpotifyAuthServiceTest {

	private static final String REDIRECT_URI = "http://localhost:8080/api/v1/auth/spotify/callback";
	private SpotifyAuthService spotifyAuthService;

	@BeforeEach
	void setUp() {
		SpotifyOAuthProperties properties = new SpotifyOAuthProperties(
			"https://accounts.spotify.com/authorize",
			"test-client-id",
			"test-client-secret",
			REDIRECT_URI,
			List.of("user-read-email", "playlist-read-private"),
			"setpik_spotify_oauth_state",
			"setpik_spotify_frontend_origin",
			"/api/v1/auth/spotify",
			Duration.ofMinutes(10),
			false
		);
		spotifyAuthService = new SpotifyAuthService(properties, new OAuthStateGenerator());
	}

	@Test
	void createsSpotifyLoginUrlWithRequiredParameters() {
		SpotifyLoginUrlResponse response = spotifyAuthService.createLoginUrl(REDIRECT_URI);
		Map<String, String> queryParams = parseQueryParams(response.loginUrl());

		assertThat(response.loginUrl()).startsWith("https://accounts.spotify.com/authorize?");
		assertThat(queryParams)
			.containsEntry("client_id", "test-client-id")
			.containsEntry("response_type", "code")
			.containsEntry("redirect_uri", REDIRECT_URI)
			.containsEntry("state", response.state())
			.containsEntry("scope", "user-read-email playlist-read-private");
		assertThat(response.state()).hasSize(43);
	}

	@Test
	void rejectsRedirectUriThatIsNotConfigured() {
		assertThatThrownBy(() -> spotifyAuthService.createLoginUrl("https://attacker.example/callback"))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
	}

	private Map<String, String> parseQueryParams(String loginUrl) {
		String rawQuery = URI.create(loginUrl).getRawQuery();
		return Arrays.stream(rawQuery.split("&"))
			.map(parameter -> parameter.split("=", 2))
			.collect(Collectors.toMap(
				parameter -> decode(parameter[0]),
				parameter -> decode(parameter[1])
			));
	}

	private String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}
}

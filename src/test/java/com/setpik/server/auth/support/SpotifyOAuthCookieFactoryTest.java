package com.setpik.server.auth.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.setpik.server.auth.config.SpotifyOAuthProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class SpotifyOAuthCookieFactoryTest {

	@Test
	void createsOAuthCookiesForVercelProxyPath() {
		SpotifyOAuthProperties properties = properties("/backend/v1/auth/spotify");

		ResponseCookie stateCookie = new SpotifyOAuthStateCookieFactory(properties).create("state");
		ResponseCookie frontendCookie = new SpotifyOAuthFrontendCookieFactory(properties)
			.create("https://5th-mvp-set-pik-web.vercel.app");

		assertProxyCookie(stateCookie);
		assertProxyCookie(frontendCookie);
	}

	@Test
	void deletesOAuthCookiesUsingTheSameProxyPath() {
		SpotifyOAuthProperties properties = properties("/backend/v1/auth/spotify");

		ResponseCookie stateCookie = new SpotifyOAuthStateCookieFactory(properties).delete();
		ResponseCookie frontendCookie = new SpotifyOAuthFrontendCookieFactory(properties).delete();

		assertThat(stateCookie.getPath()).isEqualTo("/backend/v1/auth/spotify");
		assertThat(frontendCookie.getPath()).isEqualTo("/backend/v1/auth/spotify");
		assertThat(stateCookie.getMaxAge()).isZero();
		assertThat(frontendCookie.getMaxAge()).isZero();
	}

	private void assertProxyCookie(ResponseCookie cookie) {
		assertThat(cookie.isHttpOnly()).isTrue();
		assertThat(cookie.isSecure()).isTrue();
		assertThat(cookie.getSameSite()).isEqualTo("Lax");
		assertThat(cookie.getPath()).isEqualTo("/backend/v1/auth/spotify");
		assertThat(cookie.getDomain()).isNull();
	}

	private SpotifyOAuthProperties properties(String cookiePath) {
		return new SpotifyOAuthProperties(
			"https://accounts.spotify.com/authorize",
			"client-id",
			"client-secret",
			"https://5th-mvp-set-pik-web.vercel.app/backend/v1/auth/spotify/callback",
			List.of("user-read-email"),
			"setpik_spotify_oauth_state",
			"setpik_spotify_frontend_origin",
			cookiePath,
			Duration.ofMinutes(10),
			true
		);
	}
}

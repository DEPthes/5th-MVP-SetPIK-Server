package com.setpik.server.auth.support;

import com.setpik.server.auth.config.SpotifyOAuthProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class SpotifyOAuthFrontendCookieFactory {

	private final SpotifyOAuthProperties properties;

	public SpotifyOAuthFrontendCookieFactory(SpotifyOAuthProperties properties) {
		this.properties = properties;
	}

	public ResponseCookie create(String frontendOrigin) {
		String encodedOrigin = Base64.getUrlEncoder().withoutPadding()
			.encodeToString(frontendOrigin.getBytes(StandardCharsets.UTF_8));

		return cookieBuilder(encodedOrigin)
			.maxAge(properties.stateCookieMaxAge())
			.build();
	}

	public String decode(String encodedOrigin) {
		if (encodedOrigin == null || encodedOrigin.isBlank()) {
			return null;
		}
		try {
			return new String(Base64.getUrlDecoder().decode(encodedOrigin), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	public ResponseCookie delete() {
		return cookieBuilder("").maxAge(0).build();
	}

	private ResponseCookie.ResponseCookieBuilder cookieBuilder(String value) {
		return ResponseCookie.from(properties.frontendCookieName(), value)
			.httpOnly(true)
			.secure(properties.secureCookie())
			.sameSite("Lax")
			.path(properties.cookiePath());
	}
}

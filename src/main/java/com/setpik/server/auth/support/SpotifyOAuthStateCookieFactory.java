package com.setpik.server.auth.support;

import com.setpik.server.auth.config.SpotifyOAuthProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class SpotifyOAuthStateCookieFactory {

	private static final String COOKIE_PATH = "/api/v1/auth/spotify";
	private final SpotifyOAuthProperties properties;

	public SpotifyOAuthStateCookieFactory(SpotifyOAuthProperties properties) {
		this.properties = properties;
	}

	/** 콜백에서 state를 검증할 수 있도록 짧은 수명의 HttpOnly 쿠키를 만든다. */
	public ResponseCookie create(String state) {
		return ResponseCookie.from(properties.stateCookieName(), state)
			.httpOnly(true)
			.secure(properties.secureCookie())
			.sameSite("Lax")
			.path(COOKIE_PATH)
			.maxAge(properties.stateCookieMaxAge())
			.build();
	}

	/** 사용이 끝난 state 쿠키를 같은 경로에서 즉시 만료시킨다. */
	public ResponseCookie delete() {
		return ResponseCookie.from(properties.stateCookieName(), "")
			.httpOnly(true)
			.secure(properties.secureCookie())
			.sameSite("Lax")
			.path(COOKIE_PATH)
			.maxAge(0)
			.build();
	}
}

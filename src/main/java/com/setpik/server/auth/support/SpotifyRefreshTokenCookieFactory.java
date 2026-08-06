package com.setpik.server.auth.support;

import com.setpik.server.auth.config.SetpikAuthProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class SpotifyRefreshTokenCookieFactory {

	private final SetpikAuthProperties properties;

	public SpotifyRefreshTokenCookieFactory(SetpikAuthProperties properties) {
		this.properties = properties;
	}

	/** SetPIK Refresh Token은 JavaScript에서 읽을 수 없는 HttpOnly 쿠키로만 전달한다. */
	public ResponseCookie create(String refreshToken) {
		return ResponseCookie.from(properties.refreshCookieName(), refreshToken)
			.httpOnly(true)
			.secure(properties.secureCookie())
			.sameSite("Lax")
			.path(properties.refreshCookiePath())
			.maxAge(properties.refreshTokenExpiration())
			.build();
	}
}

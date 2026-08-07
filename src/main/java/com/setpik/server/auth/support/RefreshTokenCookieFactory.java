package com.setpik.server.auth.support;

import com.setpik.server.auth.config.SetpikAuthProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieFactory {

	private final SetpikAuthProperties properties;

	public RefreshTokenCookieFactory(SetpikAuthProperties properties) {
		this.properties = properties;
	}

	/** SetPIK Refresh Token은 JavaScript에서 읽을 수 없는 HttpOnly 쿠키로만 전달한다. */
	public ResponseCookie create(String refreshToken) {
		return baseCookie(refreshToken)
			.maxAge(properties.refreshTokenExpiration())
			.build();
	}

	/** 로그아웃 시 생성할 때와 동일한 이름과 경로로 쿠키를 즉시 만료시킨다. */
	public ResponseCookie delete() {
		return baseCookie("")
			.maxAge(0)
			.build();
	}

	private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
		return ResponseCookie.from(properties.refreshCookieName(), value)
			.httpOnly(true)
			.secure(properties.secureCookie())
			.sameSite("Lax")
			.path(properties.refreshCookiePath());
	}
}

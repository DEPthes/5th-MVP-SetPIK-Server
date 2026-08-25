package com.setpik.server.auth.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.setpik.server.auth.config.SetpikAuthProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class RefreshTokenCookieFactoryTest {

	@Test
	void createsProductionCookieForVercelProxyRequest() {
		SetpikAuthProperties properties = properties(true, "Lax", "/backend/v1/auth");
		RefreshTokenCookieFactory factory = new RefreshTokenCookieFactory(properties);

		ResponseCookie cookie = factory.create("refresh-token");

		assertThat(cookie.isHttpOnly()).isTrue();
		assertThat(cookie.isSecure()).isTrue();
		assertThat(cookie.getSameSite()).isEqualTo("Lax");
		assertThat(cookie.getPath()).isEqualTo("/backend/v1/auth");
		assertThat(cookie.getDomain()).isNull();
	}

	@Test
	void createsLocalCookieWithLaxPolicy() {
		SetpikAuthProperties properties = properties(false, "Lax", "/api/v1/auth");
		RefreshTokenCookieFactory factory = new RefreshTokenCookieFactory(properties);

		ResponseCookie cookie = factory.create("refresh-token");

		assertThat(cookie.isSecure()).isFalse();
		assertThat(cookie.getSameSite()).isEqualTo("Lax");
	}

	private SetpikAuthProperties properties(boolean secure, String sameSite, String cookiePath) {
		return new SetpikAuthProperties(
			"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
			Duration.ofDays(14),
			"refreshToken",
			cookiePath,
			secure,
			sameSite,
			"http://localhost:3000/oauth/success",
			"http://localhost:3000/oauth/failure"
		);
	}
}

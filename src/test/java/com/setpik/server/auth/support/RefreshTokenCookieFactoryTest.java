package com.setpik.server.auth.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.setpik.server.auth.config.SetpikAuthProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class RefreshTokenCookieFactoryTest {

	@Test
	void createsProductionCookieForCrossSiteRequest() {
		SetpikAuthProperties properties = properties(true, "None");
		RefreshTokenCookieFactory factory = new RefreshTokenCookieFactory(properties);

		ResponseCookie cookie = factory.create("refresh-token");

		assertThat(cookie.isHttpOnly()).isTrue();
		assertThat(cookie.isSecure()).isTrue();
		assertThat(cookie.getSameSite()).isEqualTo("None");
		assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
	}

	@Test
	void createsLocalCookieWithLaxPolicy() {
		SetpikAuthProperties properties = properties(false, "Lax");
		RefreshTokenCookieFactory factory = new RefreshTokenCookieFactory(properties);

		ResponseCookie cookie = factory.create("refresh-token");

		assertThat(cookie.isSecure()).isFalse();
		assertThat(cookie.getSameSite()).isEqualTo("Lax");
	}

	private SetpikAuthProperties properties(boolean secure, String sameSite) {
		return new SetpikAuthProperties(
			"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
			Duration.ofDays(14),
			"refreshToken",
			"/api/v1/auth",
			secure,
			sameSite,
			"http://localhost:3000/oauth/success",
			"http://localhost:3000/oauth/failure"
		);
	}
}

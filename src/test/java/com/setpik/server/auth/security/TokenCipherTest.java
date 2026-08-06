package com.setpik.server.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.setpik.server.auth.config.SetpikAuthProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class TokenCipherTest {

	@Test
	void encryptsSameTokenDifferentlyWithRandomIv() {
		SetpikAuthProperties properties = new SetpikAuthProperties(
			"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
			Duration.ofDays(14),
			"refreshToken",
			"/api/v1/auth",
			false,
			"http://localhost:3000/oauth/success",
			"http://localhost:3000/oauth/failure"
		);
		TokenCipher tokenCipher = new TokenCipher(properties);

		String first = tokenCipher.encrypt("spotify-token");
		String second = tokenCipher.encrypt("spotify-token");

		assertThat(first).isNotEqualTo("spotify-token");
		assertThat(second).isNotEqualTo("spotify-token");
		assertThat(first).isNotEqualTo(second);
	}
}

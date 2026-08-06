package com.setpik.server.auth.support;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class OAuthStateGenerator {

	private static final int STATE_BYTE_LENGTH = 32;
	private final SecureRandom secureRandom = new SecureRandom();

	/** 예측하기 어려운 일회성 state를 만들어 OAuth 요청 위조를 방지한다. */
	public String generate() {
		byte[] randomBytes = new byte[STATE_BYTE_LENGTH];
		secureRandom.nextBytes(randomBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}
}

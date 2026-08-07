package com.setpik.server.auth.security;

import com.setpik.server.auth.config.SetpikAuthProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class TokenCipher {

	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int IV_LENGTH = 12;
	private static final int AUTH_TAG_LENGTH = 128;
	private static final int AES_KEY_LENGTH = 32;
	private final SecureRandom secureRandom = new SecureRandom();
	private final SecretKeySpec secretKey;

	public TokenCipher(SetpikAuthProperties properties) {
		byte[] keyBytes = Base64.getDecoder().decode(properties.tokenEncryptionKey());
		if (keyBytes.length != AES_KEY_LENGTH) {
			throw new IllegalStateException("TOKEN_ENCRYPTION_KEY는 Base64로 인코딩된 32바이트 키여야 합니다.");
		}
		this.secretKey = new SecretKeySpec(keyBytes, "AES");
	}

	/** 매번 새로운 IV를 사용하는 AES-GCM으로 Spotify 토큰을 암호화한다. */
	public String encrypt(String plainText) {
		if (plainText == null || plainText.isBlank()) {
			return null;
		}

		try {
			byte[] iv = new byte[IV_LENGTH];
			secureRandom.nextBytes(iv);
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(AUTH_TAG_LENGTH, iv));
			byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

			return Base64.getEncoder().encodeToString(
				ByteBuffer.allocate(iv.length + encrypted.length)
					.put(iv)
					.put(encrypted)
					.array()
			);
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Spotify 토큰 암호화에 실패했습니다.", exception);
		}
	}

	/** DB에 암호화해 저장한 Spotify 토큰을 외부 API 호출 직전에만 복호화한다. */
	public String decrypt(String encryptedText) {
		if (encryptedText == null || encryptedText.isBlank()) {
			return null;
		}

		try {
			byte[] combined = Base64.getDecoder().decode(encryptedText);
			if (combined.length <= IV_LENGTH) {
				throw new IllegalStateException("암호화된 Spotify 토큰 형식이 올바르지 않습니다.");
			}

			ByteBuffer buffer = ByteBuffer.wrap(combined);
			byte[] iv = new byte[IV_LENGTH];
			buffer.get(iv);
			byte[] encrypted = new byte[buffer.remaining()];
			buffer.get(encrypted);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(AUTH_TAG_LENGTH, iv));
			return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException | IllegalArgumentException exception) {
			throw new IllegalStateException("Spotify 토큰 복호화에 실패했습니다.", exception);
		}
	}
}

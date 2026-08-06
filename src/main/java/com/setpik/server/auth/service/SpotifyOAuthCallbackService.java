package com.setpik.server.auth.service;

import com.setpik.server.auth.client.SpotifyApiException;
import com.setpik.server.auth.client.SpotifyOAuthClient;
import com.setpik.server.auth.client.dto.SpotifyProfileResponse;
import com.setpik.server.auth.client.dto.SpotifyTokenResponse;
import com.setpik.server.auth.dto.SpotifyCallbackResult;
import com.setpik.server.auth.exception.SpotifyOAuthCallbackException;
import com.setpik.server.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SpotifyOAuthCallbackService {

	private static final Logger log = LoggerFactory.getLogger(SpotifyOAuthCallbackService.class);
	private final SpotifyOAuthClient spotifyOAuthClient;
	private final SpotifyLoginPersistenceService loginPersistenceService;

	public SpotifyOAuthCallbackService(
		SpotifyOAuthClient spotifyOAuthClient,
		SpotifyLoginPersistenceService loginPersistenceService
	) {
		this.spotifyOAuthClient = spotifyOAuthClient;
		this.loginPersistenceService = loginPersistenceService;
	}

	/** state 검증 후 Spotify 인증 결과를 조회하고 내부 로그인 데이터를 저장한다. */
	public SpotifyCallbackResult handleCallback(
		String code,
		String state,
		String storedState,
		String authorizationError
	) {
		validateCallback(code, state, storedState, authorizationError);

		try {
			SpotifyTokenResponse tokenResponse = spotifyOAuthClient.exchangeCode(code);
			SpotifyProfileResponse profileResponse = spotifyOAuthClient
				.getCurrentUser(tokenResponse.accessToken());
			return loginPersistenceService.saveLogin(tokenResponse, profileResponse);
		} catch (SpotifyApiException exception) {
			throw new SpotifyOAuthCallbackException(ErrorCode.SPOTIFY_API_ERROR);
		}
	}

	private void validateCallback(
		String code,
		String state,
		String storedState,
		String authorizationError
	) {
		if (!isBlank(authorizationError)) {
			log.warn("Spotify OAuth 인가 실패: error={}", authorizationError);
			throw new SpotifyOAuthCallbackException(ErrorCode.INVALID_REQUEST);
		}
		if (isBlank(code)) {
			log.warn("Spotify OAuth 콜백 검증 실패: authorization code가 없습니다.");
			throw new SpotifyOAuthCallbackException(ErrorCode.INVALID_REQUEST);
		}
		if (isBlank(state)) {
			log.warn("Spotify OAuth 콜백 검증 실패: state 쿼리 값이 없습니다.");
			throw new SpotifyOAuthCallbackException(ErrorCode.INVALID_REQUEST);
		}
		if (isBlank(storedState)) {
			log.warn("Spotify OAuth 콜백 검증 실패: state 쿠키가 없습니다. 로그인 URL 발급 주소와 콜백 호스트를 확인하세요.");
			throw new SpotifyOAuthCallbackException(ErrorCode.INVALID_REQUEST);
		}
		if (!constantTimeEquals(state, storedState)) {
			log.warn("Spotify OAuth 콜백 검증 실패: state 쿼리 값과 쿠키 값이 일치하지 않습니다.");
			throw new SpotifyOAuthCallbackException(ErrorCode.INVALID_REQUEST);
		}
	}

	private boolean constantTimeEquals(String left, String right) {
		return MessageDigest.isEqual(
			left.getBytes(StandardCharsets.UTF_8),
			right.getBytes(StandardCharsets.UTF_8)
		);
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}

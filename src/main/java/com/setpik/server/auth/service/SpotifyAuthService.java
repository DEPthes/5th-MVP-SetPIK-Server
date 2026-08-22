package com.setpik.server.auth.service;

import com.setpik.server.auth.config.SpotifyOAuthProperties;
import com.setpik.server.auth.dto.SpotifyLoginUrlResponse;
import com.setpik.server.auth.support.OAuthStateGenerator;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SpotifyAuthService {

	private final SpotifyOAuthProperties properties;
	private final OAuthStateGenerator stateGenerator;

	public SpotifyAuthService(
		SpotifyOAuthProperties properties,
		OAuthStateGenerator stateGenerator
	) {
		this.properties = properties;
		this.stateGenerator = stateGenerator;
	}

	/**
	 * 허용된 콜백 주소인지 확인하고 Spotify 인가 페이지 URL을 생성한다.
	 * 이 단계에서는 DB에 저장할 데이터가 없으므로 Repository를 호출하지 않는다.
	 */
	public SpotifyLoginUrlResponse createLoginUrl(String redirectUri) {
		String validatedRedirectUri = validateRedirectUri(redirectUri);
		return createLoginUrlWithRedirectUri(validatedRedirectUri);
	}

	/** 브라우저 직접 이동 방식에서는 서버에 등록된 콜백 URI를 사용한다. */
	public SpotifyLoginUrlResponse createLoginUrl() {
		return createLoginUrlWithRedirectUri(properties.redirectUri());
	}

	private SpotifyLoginUrlResponse createLoginUrlWithRedirectUri(String redirectUri) {
		String state = stateGenerator.generate();
		String loginUrl = UriComponentsBuilder.fromUriString(properties.authorizationUri())
			.queryParam("client_id", properties.clientId())
			.queryParam("response_type", "code")
			.queryParam("redirect_uri", redirectUri)
			.queryParam("state", state)
			.queryParam("scope", String.join(" ", properties.scopes()))
			.build()
			.encode(StandardCharsets.UTF_8)
			.toUriString();

		return new SpotifyLoginUrlResponse(loginUrl, state);
	}

	private String validateRedirectUri(String redirectUri) {
		String trimmedRedirectUri = redirectUri == null ? "" : redirectUri.trim();
		if (!properties.redirectUri().equals(trimmedRedirectUri)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
		return trimmedRedirectUri;
	}
}

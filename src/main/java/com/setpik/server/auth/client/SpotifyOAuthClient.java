package com.setpik.server.auth.client;

import com.setpik.server.auth.client.dto.SpotifyProfileResponse;
import com.setpik.server.auth.client.dto.SpotifyTokenResponse;
import com.setpik.server.auth.config.SpotifyOAuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class SpotifyOAuthClient {

	private static final Logger log = LoggerFactory.getLogger(SpotifyOAuthClient.class);
	private static final String TOKEN_URI = "https://accounts.spotify.com/api/token";
	private static final String PROFILE_URI = "https://api.spotify.com/v1/me";
	private final RestClient restClient;
	private final SpotifyOAuthProperties properties;

	public SpotifyOAuthClient(RestClient.Builder restClientBuilder, SpotifyOAuthProperties properties) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
	}

	/** Authorization Code를 Spotify Access/Refresh Token으로 교환한다. */
	public SpotifyTokenResponse exchangeCode(String code) {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("grant_type", "authorization_code");
		formData.add("code", code);
		formData.add("redirect_uri", properties.redirectUri());

		try {
			SpotifyTokenResponse response = restClient.post()
				.uri(TOKEN_URI)
				.headers(headers -> headers.setBasicAuth(properties.clientId(), properties.clientSecret()))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(formData)
				.retrieve()
				.body(SpotifyTokenResponse.class);
			validateTokenResponse(response);
			return response;
		} catch (RestClientResponseException exception) {
			logSpotifyError("토큰 교환", exception);
			throw new SpotifyApiException("Spotify 토큰 교환에 실패했습니다.", exception);
		} catch (RestClientException exception) {
			throw new SpotifyApiException("Spotify 토큰 교환에 실패했습니다.", exception);
		}
	}

	/** 만료된 Spotify Access Token을 저장된 Refresh Token으로 갱신한다. */
	public SpotifyTokenResponse refreshAccessToken(String refreshToken) {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("grant_type", "refresh_token");
		formData.add("refresh_token", refreshToken);

		try {
			SpotifyTokenResponse response = restClient.post()
				.uri(TOKEN_URI)
				.headers(headers -> headers.setBasicAuth(properties.clientId(), properties.clientSecret()))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(formData)
				.retrieve()
				.body(SpotifyTokenResponse.class);
			validateTokenResponse(response);
			return response;
		} catch (RestClientResponseException exception) {
			logSpotifyError("토큰 갱신", exception);
			throw new SpotifyApiException("Spotify 토큰 갱신에 실패했습니다.", exception);
		} catch (RestClientException exception) {
			throw new SpotifyApiException("Spotify 토큰 갱신에 실패했습니다.", exception);
		}
	}

	/** 발급받은 Access Token으로 Spotify 회원 프로필을 조회한다. */
	public SpotifyProfileResponse getCurrentUser(String accessToken) {
		try {
			SpotifyProfileResponse response = restClient.get()
				.uri(PROFILE_URI)
				.headers(headers -> headers.setBearerAuth(accessToken))
				.retrieve()
				.body(SpotifyProfileResponse.class);
			if (response == null || response.accountIdentifier() == null
				|| response.accountIdentifier().isBlank()) {
				throw new SpotifyApiException("Spotify 프로필 응답에 사용자 ID가 없습니다.");
			}
			return response;
		} catch (RestClientResponseException exception) {
			logSpotifyError("프로필 조회", exception);
			throw new SpotifyApiException("Spotify 프로필 조회에 실패했습니다.", exception);
		} catch (RestClientException exception) {
			throw new SpotifyApiException("Spotify 프로필 조회에 실패했습니다.", exception);
		}
	}

	private void validateTokenResponse(SpotifyTokenResponse response) {
		if (response == null || response.accessToken() == null || response.accessToken().isBlank()
			|| response.expiresIn() <= 0) {
			throw new SpotifyApiException("Spotify 토큰 응답이 올바르지 않습니다.");
		}
	}

	private void logSpotifyError(String operation, RestClientResponseException exception) {
		String responseBody = exception.getResponseBodyAsString();
		if (responseBody.length() > 500) {
			responseBody = responseBody.substring(0, 500);
		}
		log.warn(
			"Spotify {} 실패: status={}, response={}",
			operation,
			exception.getStatusCode().value(),
			responseBody
		);
	}
}

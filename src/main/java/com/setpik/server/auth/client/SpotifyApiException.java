package com.setpik.server.auth.client;

/** Spotify 토큰 교환 또는 프로필 조회가 실패했음을 Service에 전달한다. */
public class SpotifyApiException extends RuntimeException {

	public SpotifyApiException(String message, Throwable cause) {
		super(message, cause);
	}

	public SpotifyApiException(String message) {
		super(message);
	}
}

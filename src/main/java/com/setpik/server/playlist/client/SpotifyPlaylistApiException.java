package com.setpik.server.playlist.client;

import org.springframework.web.client.RestClientResponseException;

public class SpotifyPlaylistApiException extends RuntimeException {

	public SpotifyPlaylistApiException(String message, Throwable cause) {
		super(message, cause);
	}

	public SpotifyPlaylistApiException(String message) {
		super(message);
	}

	public boolean requiresReauthentication() {
		Throwable current = getCause();
		while (current != null) {
			if (current instanceof RestClientResponseException responseException) {
				int status = responseException.getStatusCode().value();
				return status == 401 || status == 403;
			}
			current = current.getCause();
		}
		return false;
	}
}

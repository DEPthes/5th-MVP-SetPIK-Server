package com.setpik.server.playlist.client;

public class SpotifyPlaylistApiException extends RuntimeException {

	public SpotifyPlaylistApiException(String message, Throwable cause) {
		super(message, cause);
	}

	public SpotifyPlaylistApiException(String message) {
		super(message);
	}
}

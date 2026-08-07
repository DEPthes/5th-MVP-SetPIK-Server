package com.setpik.server.auth.dto;

/** API 명세서의 Spotify 로그인 URL 조회 result 객체다. */
public record SpotifyLoginUrlResponse(
	String loginUrl,
	String state
) {
}

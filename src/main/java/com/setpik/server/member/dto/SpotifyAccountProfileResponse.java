package com.setpik.server.member.dto;

public record SpotifyAccountProfileResponse(
	String spotifyUserId,
	String displayName,
	String profileImageUrl
) {
}

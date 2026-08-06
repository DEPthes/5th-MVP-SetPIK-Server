package com.setpik.server.member.dto;

import com.setpik.server.member.domain.UserStatus;
import java.time.OffsetDateTime;

public record UserProfileResponse(
	Long userId,
	UserStatus status,
	OffsetDateTime lastLoginAt,
	boolean spotifyConnected,
	SpotifyAccountProfileResponse spotifyAccount
) {
}

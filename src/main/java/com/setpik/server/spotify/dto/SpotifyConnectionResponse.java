package com.setpik.server.spotify.dto;

import com.setpik.server.spotify.domain.ConnectionStatus;
import java.time.OffsetDateTime;
import java.util.List;

public record SpotifyConnectionResponse(
	boolean connected,
	ConnectionStatus connectionStatus,
	OffsetDateTime tokenExpiresAt,
	List<SpotifyScopeResponse> scopes
) {
}

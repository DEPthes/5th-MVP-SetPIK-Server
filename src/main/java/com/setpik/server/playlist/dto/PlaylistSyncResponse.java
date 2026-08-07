package com.setpik.server.playlist.dto;

import java.time.OffsetDateTime;

public record PlaylistSyncResponse(
	int syncedPlaylistCount,
	int syncedTrackCount,
	OffsetDateTime lastSyncedAt
) {
}

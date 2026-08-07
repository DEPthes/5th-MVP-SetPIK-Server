package com.setpik.server.playlist.dto;

public record PlaylistSyncResponse(
	int syncedPlaylistCount,
	int syncedTrackCount
) {
}

package com.setpik.server.playlist.dto;

import com.setpik.server.playlist.domain.SpotifyPlaylist;
import java.time.LocalDateTime;

public record PlaylistSummaryResponse(
	Long playlistId,
	String playlistName,
	String coverImageUrl,
	Integer trackCount,
	LocalDateTime lastSyncedAt
) {
	public static PlaylistSummaryResponse from(SpotifyPlaylist playlist) {
		return new PlaylistSummaryResponse(
			playlist.getPlaylistId(),
			playlist.getPlaylistName(),
			playlist.getCoverImageUrl(),
			playlist.getTrackCount(),
			playlist.getLastSyncedAt()
		);
	}
}

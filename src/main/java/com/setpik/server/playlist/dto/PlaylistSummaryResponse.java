package com.setpik.server.playlist.dto;

import com.setpik.server.playlist.domain.SpotifyPlaylist;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record PlaylistSummaryResponse(
	Long playlistId,
	String spotifyPlaylistId,
	String playlistName,
	Integer trackCount,
	String coverImageUrl,
	OffsetDateTime lastSyncedAt
) {
	public static PlaylistSummaryResponse from(SpotifyPlaylist playlist) {
		return new PlaylistSummaryResponse(
			playlist.getPlaylistId(),
			playlist.getSpotifyPlaylistId(),
			playlist.getPlaylistName(),
			playlist.getTrackCount(),
			playlist.getCoverImageUrl(),
			playlist.getLastSyncedAt() == null
				? null
				: playlist.getLastSyncedAt().atOffset(ZoneOffset.ofHours(9))
		);
	}
}

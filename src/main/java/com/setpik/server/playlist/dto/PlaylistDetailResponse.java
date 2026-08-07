package com.setpik.server.playlist.dto;

import com.setpik.server.playlist.domain.SpotifyPlaylist;
import java.time.LocalDateTime;

public record PlaylistDetailResponse(
	Long playlistId,
	String spotifyPlaylistId,
	String playlistName,
	String description,
	String coverImageUrl,
	Boolean isPublic,
	String ownerSpotifyUserId,
	Integer trackCount,
	LocalDateTime lastSyncedAt
) {
	public static PlaylistDetailResponse from(SpotifyPlaylist playlist) {
		return new PlaylistDetailResponse(
			playlist.getPlaylistId(),
			playlist.getSpotifyPlaylistId(),
			playlist.getPlaylistName(),
			playlist.getDescription(),
			playlist.getCoverImageUrl(),
			playlist.getIsPublic(),
			playlist.getOwnerSpotifyUserId(),
			playlist.getTrackCount(),
			playlist.getLastSyncedAt()
		);
	}
}

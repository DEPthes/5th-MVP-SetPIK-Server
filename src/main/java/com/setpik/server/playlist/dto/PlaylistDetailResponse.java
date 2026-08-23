package com.setpik.server.playlist.dto;

import com.setpik.server.playlist.domain.SpotifyPlaylist;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record PlaylistDetailResponse(
	Long playlistId,
	String spotifyPlaylistId,
	String playlistName,
	String description,
	String coverImageUrl,
	Integer trackCount,
	Boolean isPublic,
	String ownerSpotifyUserId,
	boolean analysisAvailable,
	OffsetDateTime lastSyncedAt,
	OffsetDateTime deletedAt
) {
	public static PlaylistDetailResponse from(SpotifyPlaylist playlist) {
		return new PlaylistDetailResponse(
			playlist.getPlaylistId(),
			playlist.getSpotifyPlaylistId(),
			playlist.getPlaylistName(),
			playlist.getDescription(),
			playlist.getCoverImageUrl(),
			playlist.getTrackCount(),
			playlist.getIsPublic(),
			playlist.getOwnerSpotifyUserId(),
			playlist.getTrackCount() != null && playlist.getTrackCount() > 0,
			toOffsetDateTime(playlist.getLastSyncedAt()),
			toOffsetDateTime(playlist.getDeletedAt())
		);
	}

	private static OffsetDateTime toOffsetDateTime(java.time.LocalDateTime value) {
		return value == null ? null : value.atOffset(ZoneOffset.ofHours(9));
	}
}

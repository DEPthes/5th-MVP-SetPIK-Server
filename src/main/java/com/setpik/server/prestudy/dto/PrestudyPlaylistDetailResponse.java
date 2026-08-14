package com.setpik.server.prestudy.dto;

import com.setpik.server.prestudy.domain.PrestudyPlaylist;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record PrestudyPlaylistDetailResponse(
	Long prestudyPlaylistId,
	String spotifyPlaylistId,
	String playlistTitle,
	Boolean isPublic,
	Integer trackCount,
	String creationStatus,
	OffsetDateTime createdAt,
	Boolean spotifyDeleted
) {
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public static PrestudyPlaylistDetailResponse from(PrestudyPlaylist playlist) {
		return new PrestudyPlaylistDetailResponse(
			playlist.getPrestudyPlaylistId(),
			playlist.getSpotifyPlaylistId(),
			playlist.getPlaylistTitle(),
			playlist.getIsPublic(),
			playlist.getTrackCount(),
			playlist.getCreationStatus().name(),
			playlist.getCreatedAt().atZone(KST).toOffsetDateTime(),
			playlist.getSpotifyDeleted()
		);
	}
}
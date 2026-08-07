package com.setpik.server.playlist.client.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Spotify 플레이리스트 항목 중 DB에 저장할 트랙 필드만 담는다. */
public record SpotifyTrackSnapshot(
	String spotifyTrackId,
	String trackName,
	String albumName,
	String albumImageUrl,
	String spotifyTrackUrl,
	String previewUrl,
	Integer durationMs,
	Boolean isPlayable,
	LocalDateTime addedAt,
	List<SpotifyArtistSnapshot> artists
) {
}

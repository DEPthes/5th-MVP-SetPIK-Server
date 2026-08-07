package com.setpik.server.playlist.client.dto;

import java.util.List;

/** Spotify 응답을 서비스가 사용하기 쉬운 형태로 정규화한 플레이리스트 데이터다. */
public record SpotifyPlaylistSnapshot(
	String spotifyPlaylistId,
	String playlistName,
	String description,
	String coverImageUrl,
	Boolean isPublic,
	String ownerSpotifyUserId,
	String snapshotId,
	List<SpotifyTrackSnapshot> tracks
) {
}

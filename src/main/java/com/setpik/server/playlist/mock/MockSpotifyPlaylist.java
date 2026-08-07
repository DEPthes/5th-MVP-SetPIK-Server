package com.setpik.server.playlist.mock;

import java.util.List;

/** Spotify API 응답을 흉내 낸 Mock 데이터 구조. 실제 연동 시 이 자리를 API 응답 DTO로 교체한다. */
public record MockSpotifyPlaylist(
	String spotifyPlaylistId,
	String playlistName,
	String description,
	String coverImageUrl,
	Boolean isPublic,
	String ownerSpotifyUserId,
	String snapshotId,
	List<MockSpotifyTrack> tracks
) {
}

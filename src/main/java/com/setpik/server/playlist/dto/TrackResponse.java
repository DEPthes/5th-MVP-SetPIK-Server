package com.setpik.server.playlist.dto;

import com.setpik.server.playlist.domain.Track;
import com.setpik.server.playlist.domain.PlaylistTrack;
import java.time.LocalDateTime;
import java.util.List;

public record TrackResponse(
	Long playlistTrackId,
	Integer trackPosition,
	String trackName,
	String spotifyTrackId,
	String albumName,
	String albumImageUrl,
	LocalDateTime addedAt,
	Integer durationMs,
	List<TrackArtistResponse> artists
) {
	public static TrackResponse of(
		PlaylistTrack playlistTrack,
		Track track,
		List<TrackArtistResponse> artists
	) {
		return new TrackResponse(
			playlistTrack.getPlaylistTrackId(),
			playlistTrack.getTrackPosition(),
			track.getTrackName(),
			track.getSpotifyTrackId(),
			track.getAlbumName(),
			track.getAlbumImageUrl(),
			playlistTrack.getAddedAt(),
			track.getDurationMs(),
			artists
		);
	}
}

package com.setpik.server.playlist.dto;

import com.setpik.server.playlist.domain.Track;

public record TrackResponse(
	Long trackId,
	Integer trackPosition,
	String trackName,
	String albumName,
	String albumImageUrl,
	String spotifyTrackUrl,
	Integer durationMs
) {
	public static TrackResponse of(Track track, Integer position) {
		return new TrackResponse(
			track.getTrackId(),
			position,
			track.getTrackName(),
			track.getAlbumName(),
			track.getAlbumImageUrl(),
			track.getSpotifyTrackUrl(),
			track.getDurationMs()
		);
	}
}

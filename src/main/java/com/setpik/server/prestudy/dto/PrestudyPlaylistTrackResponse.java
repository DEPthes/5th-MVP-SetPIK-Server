package com.setpik.server.prestudy.dto;

import com.setpik.server.playlist.domain.Track;
import com.setpik.server.prestudy.domain.PrestudyPlaylistTrack;

public record PrestudyPlaylistTrackResponse(
	Long trackId,
	String trackName,
	String artistName,
	String albumName,
	String albumImageUrl,
	Integer durationMs,
	String spotifyTrackId,
	String spotifyTrackUrl,
	String previewUrl,
	Integer trackOrder,
	String sourceType,
	Boolean isNewArtistTrack
) {
	public static PrestudyPlaylistTrackResponse of(
		PrestudyPlaylistTrack playlistTrack,
		Track track,
		String artistName
	) {
		return new PrestudyPlaylistTrackResponse(
			track.getTrackId(),
			track.getTrackName(),
			artistName,
			track.getAlbumName(),
			track.getAlbumImageUrl(),
			track.getDurationMs(),
			track.getSpotifyTrackId(),
			track.getSpotifyTrackUrl(),
			track.getPreviewUrl(),
			playlistTrack.getTrackOrder(),
			playlistTrack.getSourceType().name(),
			playlistTrack.getIsNewArtistTrack()
		);
	}
}

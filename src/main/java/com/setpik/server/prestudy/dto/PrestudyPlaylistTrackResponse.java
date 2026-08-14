package com.setpik.server.prestudy.dto;

import com.setpik.server.playlist.domain.Track;
import com.setpik.server.prestudy.domain.PrestudyPlaylistTrack;

public record PrestudyPlaylistTrackResponse(
	Long trackId,
	String trackName,
	Integer trackOrder,
	String sourceType,
	Boolean isNewArtistTrack
) {
	public static PrestudyPlaylistTrackResponse of(PrestudyPlaylistTrack playlistTrack, Track track) {
		return new PrestudyPlaylistTrackResponse(
			track.getTrackId(),
			track.getTrackName(),
			playlistTrack.getTrackOrder(),
			playlistTrack.getSourceType().name(),
			playlistTrack.getIsNewArtistTrack()
		);
	}
}
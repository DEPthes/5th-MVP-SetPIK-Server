package com.setpik.server.prestudy.dto;

import com.setpik.server.prestudy.domain.PrestudyPlaylist;

public record PrestudyPlaylistCardStatus(
	Long prestudyPlaylistId,
	String creationStatus,
	String spotifyPlaylistId
) {
	public static PrestudyPlaylistCardStatus from(PrestudyPlaylist playlist) {
		return new PrestudyPlaylistCardStatus(
			playlist.getPrestudyPlaylistId(),
			playlist.getCreationStatus().name(),
			playlist.getSpotifyPlaylistId()
		);
	}
}

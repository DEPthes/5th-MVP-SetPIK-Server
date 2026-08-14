package com.setpik.server.prestudy.dto;

public record CreatePrestudyPlaylistResponse(
	Long prestudyPlaylistId,
	String spotifyPlaylistId,
	Integer trackCount
) {
}
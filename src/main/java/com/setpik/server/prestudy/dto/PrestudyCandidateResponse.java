package com.setpik.server.prestudy.dto;

import java.util.List;

public record PrestudyCandidateResponse(
	Long performanceId,
	Long analysisId,
	List<ArtistCandidate> artists
) {
	public record ArtistCandidate(
		Long artistId,
		String artistName,
		Boolean isFromOriginalPlaylist,
		List<TrackCandidate> candidateTracks
	) {
	}

	public record TrackCandidate(
		Long trackId,
		String trackName,
		String sourceType
	) {
	}
}
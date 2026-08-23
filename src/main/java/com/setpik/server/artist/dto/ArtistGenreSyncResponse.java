package com.setpik.server.artist.dto;

import java.time.OffsetDateTime;

public record ArtistGenreSyncResponse(
	int candidateArtistCount,
	int resolvedArtistCount,
	int savedGenreCount,
	int notFoundCount,
	int failedCount,
	OffsetDateTime completedAt
) {
}

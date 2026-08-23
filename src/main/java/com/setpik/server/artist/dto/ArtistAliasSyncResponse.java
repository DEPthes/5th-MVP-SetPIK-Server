package com.setpik.server.artist.dto;

import java.time.OffsetDateTime;

public record ArtistAliasSyncResponse(
	int candidateArtistCount,
	int resolvedAliasCount,
	int notFoundCount,
	int ambiguousCount,
	int failedCount,
	OffsetDateTime completedAt
) {
}

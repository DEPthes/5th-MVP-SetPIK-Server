package com.setpik.server.analysis.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record ArtistSelectionCompleteResponse(
	Long analysisId,
	OffsetDateTime completedAt
) {
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public static ArtistSelectionCompleteResponse of(Long analysisId, LocalDateTime completedAt) {
		return new ArtistSelectionCompleteResponse(
			analysisId,
			completedAt.atZone(KST).toOffsetDateTime()
		);
	}
}

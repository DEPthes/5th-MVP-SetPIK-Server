package com.setpik.server.kopis.dto;

import java.time.LocalDateTime;

/** 기존 KOPIS 출연진의 Spotify 아티스트 재연결 결과다. */
public record KopisArtistBackfillResponse(
	int candidateArtistCount,
	int matchedArtistCount,
	int remappedPerformanceArtistCount,
	int unmatchedArtistCount,
	Long nextAfterArtistId,
	boolean retryRequired,
	LocalDateTime completedAt
) {
}

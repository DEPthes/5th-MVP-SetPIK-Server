package com.setpik.server.prestudy.dto;

import com.setpik.server.prestudy.domain.PrestudyPlaylist;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record PrestudyPlaylistSummaryResponse(
	Long prestudyPlaylistId,
	String playlistTitle,
	Long performanceId,
	String performanceName,
	String posterUrl,
	Integer trackCount,
	String creationStatus,
	OffsetDateTime createdAt
) {
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public static PrestudyPlaylistSummaryResponse of(PrestudyPlaylist playlist, String performanceName, String posterUrl) {
		return new PrestudyPlaylistSummaryResponse(
			playlist.getPrestudyPlaylistId(),
			playlist.getPlaylistTitle(),
			playlist.getPerformanceId(),
			performanceName,
			posterUrl,
			playlist.getTrackCount(),
			playlist.getCreationStatus().name(),
			playlist.getCreatedAt().atZone(KST).toOffsetDateTime()
		);
	}
}
package com.setpik.server.playlist.dto;

import com.setpik.server.playlist.domain.PlaylistRecentSelection;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record PlaylistSelectResponse(
	Long playlistId,
	OffsetDateTime selectedAt
) {
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public static PlaylistSelectResponse from(PlaylistRecentSelection selection) {
		return new PlaylistSelectResponse(
			selection.getPlaylistId(),
			selection.getSelectedAt().atZone(KST).toOffsetDateTime()
		);
	}
}

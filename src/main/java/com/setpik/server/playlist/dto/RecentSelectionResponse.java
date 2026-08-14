package com.setpik.server.playlist.dto;

import com.setpik.server.playlist.domain.PlaylistRecentSelection;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record RecentSelectionResponse(
	Long playlistId,
	String playlistName,
	OffsetDateTime selectedAt
) {
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public static RecentSelectionResponse from(
		PlaylistRecentSelection selection,
		String playlistName
	) {
		return new RecentSelectionResponse(
			selection.getPlaylistId(),
			playlistName,
			selection.getSelectedAt().atZone(KST).toOffsetDateTime()
		);
	}
}

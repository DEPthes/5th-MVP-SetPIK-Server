package com.setpik.server.playlist.dto;

import com.setpik.server.playlist.domain.PlaylistRecentSelection;
import java.time.LocalDateTime;

public record PlaylistSelectResponse(
	Long playlistId,
	LocalDateTime selectedAt
) {
	public static PlaylistSelectResponse from(PlaylistRecentSelection selection) {
		return new PlaylistSelectResponse(
			selection.getPlaylistId(),
			selection.getSelectedAt()
		);
	}
}

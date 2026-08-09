package com.setpik.server.playlist.dto;

import java.time.LocalDateTime;

public record RecentSelectionResponse(
	Long playlistId,
	String playlistName,
	LocalDateTime selectedAt
) {
}

package com.setpik.server.playlist.domain;

import java.io.Serializable;
import java.util.Objects;

public class PlaylistRecentSelectionId implements Serializable {

	private Long userId;
	private Long playlistId;

	public PlaylistRecentSelectionId() {
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof PlaylistRecentSelectionId that)) return false;
		return Objects.equals(userId, that.userId)
			&& Objects.equals(playlistId, that.playlistId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, playlistId);
	}
}

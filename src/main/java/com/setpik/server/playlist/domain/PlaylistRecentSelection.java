package com.setpik.server.playlist.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Flyway의 Playlist_Recent_Selections 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Playlist_Recent_Selections")
@IdClass(PlaylistRecentSelectionId.class)
public class PlaylistRecentSelection {

	@Id
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Id
	@Column(name = "playlist_id", nullable = false)
	private Long playlistId;

	@Column(name = "selected_at", nullable = false)
	private LocalDateTime selectedAt;

	protected PlaylistRecentSelection() {
	}

	public PlaylistRecentSelection(Long userId, Long playlistId, LocalDateTime selectedAt) {
		this.userId = userId;
		this.playlistId = playlistId;
		this.selectedAt = selectedAt;
	}

	public void reselect(LocalDateTime selectedAt) {
		this.selectedAt = selectedAt;
	}

	public Long getUserId() {
		return userId;
	}

	public Long getPlaylistId() {
		return playlistId;
	}

	public LocalDateTime getSelectedAt() {
		return selectedAt;
	}

}

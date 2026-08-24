package com.setpik.server.analysis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Flyway의 Playlist_Analyses 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Playlist_Analyses")
public class PlaylistAnalysis {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "analysis_id", nullable = false)
	private Long analysisId;

	@Column(name = "spotify_playlist_id_snapshot", nullable = false, length = 255)
	private String spotifyPlaylistIdSnapshot;

	@Column(name = "playlist_name_snapshot", nullable = false, length = 255)
	private String playlistNameSnapshot;

	@Column(name = "playlist_image_snapshot", nullable = true, length = 255)
	private String playlistImageSnapshot;

	@Column(name = "total_track_count", nullable = false)
	private Integer totalTrackCount;

	@Column(name = "selected_artist_count", nullable = false)
	private Integer selectedArtistCount;

	@Enumerated(EnumType.STRING)
	@Column(name = "analysis_status", nullable = false, length = 50)
	private AnalysisStatus analysisStatus;

	@Column(name = "warning_message", nullable = true, length = 500)
	private String warningMessage;

	@Column(name = "analyzed_at", nullable = false)
	private LocalDateTime analyzedAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "artist_selection_completed_at")
	private LocalDateTime artistSelectionCompletedAt;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "playlist_id", nullable = false)
	private Long playlistId;

	protected PlaylistAnalysis() {
	}

	public PlaylistAnalysis(Long userId, Long playlistId, String spotifyPlaylistIdSnapshot,
							String playlistNameSnapshot, String playlistImageSnapshot,
							Integer totalTrackCount, Integer selectedArtistCount,
							AnalysisStatus analysisStatus, String warningMessage) {
		this.userId = userId;
		this.playlistId = playlistId;
		this.spotifyPlaylistIdSnapshot = spotifyPlaylistIdSnapshot;
		this.playlistNameSnapshot = playlistNameSnapshot;
		this.playlistImageSnapshot = playlistImageSnapshot;
		this.totalTrackCount = totalTrackCount;
		this.selectedArtistCount = selectedArtistCount;
		this.analysisStatus = analysisStatus;
		this.warningMessage = warningMessage;
		this.analyzedAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}

	public void updateSelectedArtistCount(Integer selectedArtistCount) {
		this.selectedArtistCount = selectedArtistCount;
		this.updatedAt = LocalDateTime.now();
	}

	public void completeArtistSelection() {
		if (artistSelectionCompletedAt != null) {
			return;
		}
		LocalDateTime completedAt = LocalDateTime.now();
		this.artistSelectionCompletedAt = completedAt;
		this.updatedAt = completedAt;
	}

	public Long getAnalysisId() {
		return analysisId;
	}

	public String getSpotifyPlaylistIdSnapshot() {
		return spotifyPlaylistIdSnapshot;
	}

	public String getPlaylistNameSnapshot() {
		return playlistNameSnapshot;
	}

	public String getPlaylistImageSnapshot() {
		return playlistImageSnapshot;
	}

	public Integer getTotalTrackCount() {
		return totalTrackCount;
	}

	public Integer getSelectedArtistCount() {
		return selectedArtistCount;
	}

	public AnalysisStatus getAnalysisStatus() {
		return analysisStatus;
	}

	public String getWarningMessage() {
		return warningMessage;
	}

	public LocalDateTime getAnalyzedAt() {
		return analyzedAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public LocalDateTime getArtistSelectionCompletedAt() {
		return artistSelectionCompletedAt;
	}

	public Long getUserId() {
		return userId;
	}

	public Long getPlaylistId() {
		return playlistId;
	}

}

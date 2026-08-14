package com.setpik.server.prestudy.domain;

import com.setpik.server.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Flyway의 Prestudy_Playlists 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Prestudy_Playlists")
public class PrestudyPlaylist extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "prestudy_playlist_id", nullable = false)
	private Long prestudyPlaylistId;

	@Column(name = "spotify_playlist_id", nullable = true, length = 255)
	private String spotifyPlaylistId;

	@Column(name = "playlist_title", nullable = false, length = 255)
	private String playlistTitle;

	@Column(name = "is_public", nullable = false)
	private Boolean isPublic;

	@Column(name = "track_count", nullable = false)
	private Integer trackCount;

	@Column(name = "spotify_deleted", nullable = false)
	private Boolean spotifyDeleted;

	@Enumerated(EnumType.STRING)
	@Column(name = "creation_status", nullable = false, length = 50)
	private CreationStatus creationStatus;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "performance_id", nullable = false)
	private Long performanceId;

	@Column(name = "analysis_id", nullable = true)
	private Long analysisId;

	protected PrestudyPlaylist() {
	}

	/** 예습 플레이리스트 생성 요청을 PENDING 상태로 저장한다. */
	public PrestudyPlaylist(
		String playlistTitle,
		Boolean isPublic,
		Long userId,
		Long performanceId,
		Long analysisId
	) {
		this.playlistTitle = playlistTitle;
		this.isPublic = isPublic;
		this.trackCount = 0;
		this.spotifyDeleted = false;
		this.creationStatus = CreationStatus.PENDING;
		this.userId = userId;
		this.performanceId = performanceId;
		this.analysisId = analysisId;
	}

	/** Spotify 플레이리스트 생성과 트랙 추가가 끝나면 완료 상태로 전환한다. */
	public void markCompleted(String spotifyPlaylistId, Integer trackCount) {
		this.spotifyPlaylistId = spotifyPlaylistId;
		this.trackCount = trackCount;
		this.creationStatus = CreationStatus.COMPLETED;
	}

	/** Spotify API 호출이 실패하면 실패 상태로 남긴다. */
	public void markFailed() {
		this.creationStatus = CreationStatus.FAILED;
	}

	public Long getPrestudyPlaylistId() {
		return prestudyPlaylistId;
	}

	public String getSpotifyPlaylistId() {
		return spotifyPlaylistId;
	}

	public String getPlaylistTitle() {
		return playlistTitle;
	}

	public Boolean getIsPublic() {
		return isPublic;
	}

	public Integer getTrackCount() {
		return trackCount;
	}

	public Boolean getSpotifyDeleted() {
		return spotifyDeleted;
	}

	public CreationStatus getCreationStatus() {
		return creationStatus;
	}

	public Long getUserId() {
		return userId;
	}

	public Long getPerformanceId() {
		return performanceId;
	}

	public Long getAnalysisId() {
		return analysisId;
	}

}
package com.setpik.server.favorite.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Flyway의 Favorite_Performances 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Favorite_Performances")
public class FavoritePerformance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "favorite_id", nullable = false)
	private Long favoriteId;

	@Column(name = "saved_at", nullable = false)
	private LocalDateTime savedAt;

	@Column(name = "deleted_at", nullable = true)
	private LocalDateTime deletedAt;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "performance_id", nullable = false)
	private Long performanceId;

	protected FavoritePerformance() {
	}

	public FavoritePerformance(Long userId, Long performanceId, LocalDateTime savedAt) {
		this.userId = userId;
		this.performanceId = performanceId;
		this.savedAt = savedAt;
	}

	public void delete(LocalDateTime deletedAt) {
		this.deletedAt = deletedAt;
	}

	public void restore(LocalDateTime savedAt) {
		this.savedAt = savedAt;
		this.deletedAt = null;
	}

	public Long getFavoriteId() {
		return favoriteId;
	}

	public LocalDateTime getSavedAt() {
		return savedAt;
	}

	public LocalDateTime getDeletedAt() {
		return deletedAt;
	}

	public Long getUserId() {
		return userId;
	}

	public Long getPerformanceId() {
		return performanceId;
	}

}

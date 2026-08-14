package com.setpik.server.performanceview.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Flyway의 Performance_Views 테이블과 공연 조회 이력을 매핑한다. */
@Entity
@Table(name = "Performance_Views")
public class PerformanceView {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "view_id", nullable = false)
	private Long viewId;

	@Column(name = "viewed_at", nullable = false)
	private LocalDateTime viewedAt;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "analysis_id", nullable = false)
	private Long analysisId;

	@Column(name = "performance_id", nullable = false)
	private Long performanceId;

	protected PerformanceView() {
	}

	public PerformanceView(Long userId, Long analysisId, Long performanceId, LocalDateTime viewedAt) {
		this.userId = userId;
		this.analysisId = analysisId;
		this.performanceId = performanceId;
		this.viewedAt = viewedAt;
	}

	public void updateViewedAt(LocalDateTime viewedAt) {
		this.viewedAt = viewedAt;
	}

	public Long getViewId() {
		return viewId;
	}

	public LocalDateTime getViewedAt() {
		return viewedAt;
	}

	public Long getUserId() {
		return userId;
	}

	public Long getAnalysisId() {
		return analysisId;
	}

	public Long getPerformanceId() {
		return performanceId;
	}
}

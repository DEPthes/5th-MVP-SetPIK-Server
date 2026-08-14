package com.setpik.server.performanceview.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 최근 조회 목록에 필요한 테이블 조인 결과만 담는 내부 DTO다. */
public record PerformanceViewSummary(
	Long viewId,
	Long performanceId,
	String performanceName,
	String posterUrl,
	LocalDate startDate,
	String venueName,
	Long analysisId,
	LocalDateTime viewedAt
) {
}

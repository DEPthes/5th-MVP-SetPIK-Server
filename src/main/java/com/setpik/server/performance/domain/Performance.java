package com.setpik.server.performance.domain;

import com.setpik.server.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Flyway의 Performances 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Performances")
public class Performance extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "performance_id", nullable = false)
	private Long performanceId;

	@Column(name = "kopis_performance_id", nullable = true, length = 255)
	private String kopisPerformanceId;

	@Column(name = "performance_name", nullable = false, length = 255)
	private String performanceName;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "poster_url", nullable = true, length = 2048)
	private String posterUrl;

	@Column(name = "booking_url", nullable = true, length = 2048)
	private String bookingUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "performance_status", nullable = false, length = 50)
	private PerformanceStatus performanceStatus;

	@Column(name = "price_type", nullable = false, length = 50)
	private String priceType;

	@Column(name = "ticket_price_text", nullable = true, length = 255)
	private String ticketPriceText;

	@Column(name = "favorite_count", nullable = false)
	private Integer favoriteCount;

	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted;

	@Column(name = "last_synced_at", nullable = true)
	private LocalDateTime lastSyncedAt;

	@Column(name = "venue_id", nullable = false)
	private Long venueId;

	protected Performance() {
	}

	public Long getPerformanceId() {
		return performanceId;
	}

	public String getKopisPerformanceId() {
		return kopisPerformanceId;
	}

	public String getPerformanceName() {
		return performanceName;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public String getPosterUrl() {
		return posterUrl;
	}

	public String getBookingUrl() {
		return bookingUrl;
	}

	public PerformanceStatus getPerformanceStatus() {
		return performanceStatus;
	}

	public String getPriceType() {
		return priceType;
	}

	public String getTicketPriceText() {
		return ticketPriceText;
	}

	public Integer getFavoriteCount() {
		return favoriteCount;
	}

	public Boolean getIsDeleted() {
		return isDeleted;
	}

	public LocalDateTime getLastSyncedAt() {
		return lastSyncedAt;
	}

	public Long getVenueId() {
		return venueId;
	}

}

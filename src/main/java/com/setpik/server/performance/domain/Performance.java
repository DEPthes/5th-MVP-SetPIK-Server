package com.setpik.server.performance.domain;

import com.setpik.server.common.domain.BaseEntity;
import com.setpik.server.performance.service.TicketPriceParser;
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

	/** ticketPriceText(자유 텍스트)에서 파싱한 최저가로, DB 레벨 가격 정렬을 위해 별도 컬럼에 저장한다. */
	@Column(name = "min_ticket_price", nullable = true)
	private Integer minTicketPrice;

	@Column(name = "running_time", nullable = true, length = 255)
	private String runningTime;

	@Column(name = "age_restriction", nullable = true, length = 255)
	private String ageRestriction;

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

	public Performance(String kopisPerformanceId, String performanceName, LocalDate startDate,
		LocalDate endDate, String posterUrl, String bookingUrl, PerformanceStatus performanceStatus,
		String priceType, String ticketPriceText, LocalDateTime lastSyncedAt, Long venueId) {
		this(kopisPerformanceId, performanceName, startDate, endDate, posterUrl, bookingUrl,
			performanceStatus, priceType, ticketPriceText, null, null, lastSyncedAt, venueId);
	}

	public Performance(String kopisPerformanceId, String performanceName, LocalDate startDate,
		LocalDate endDate, String posterUrl, String bookingUrl, PerformanceStatus performanceStatus,
		String priceType, String ticketPriceText, String runningTime, String ageRestriction,
		LocalDateTime lastSyncedAt, Long venueId) {
		this.kopisPerformanceId = kopisPerformanceId;
		this.favoriteCount = 0;
		this.isDeleted = false;
		syncFromKopis(performanceName, startDate, endDate, posterUrl, bookingUrl,
			performanceStatus, priceType, ticketPriceText, runningTime, ageRestriction, lastSyncedAt, venueId);
	}

	public void syncFromKopis(String performanceName, LocalDate startDate, LocalDate endDate,
		String posterUrl, String bookingUrl, PerformanceStatus performanceStatus, String priceType,
		String ticketPriceText, LocalDateTime lastSyncedAt, Long venueId) {
		syncFromKopis(performanceName, startDate, endDate, posterUrl, bookingUrl,
			performanceStatus, priceType, ticketPriceText, null, null, lastSyncedAt, venueId);
	}

	public void syncFromKopis(String performanceName, LocalDate startDate, LocalDate endDate,
		String posterUrl, String bookingUrl, PerformanceStatus performanceStatus, String priceType,
		String ticketPriceText, String runningTime, String ageRestriction,
		LocalDateTime lastSyncedAt, Long venueId) {
		this.performanceName = performanceName;
		this.startDate = startDate;
		this.endDate = endDate;
		this.posterUrl = posterUrl;
		this.bookingUrl = bookingUrl;
		this.performanceStatus = performanceStatus;
		this.priceType = priceType;
		this.ticketPriceText = ticketPriceText;
		this.minTicketPrice = TicketPriceParser.parseMinPrice(priceType, ticketPriceText);
		this.runningTime = runningTime;
		this.ageRestriction = ageRestriction;
		this.lastSyncedAt = lastSyncedAt;
		this.venueId = venueId;
		this.isDeleted = false;
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

	public Integer getMinTicketPrice() {
		return minTicketPrice;
	}

	public String getRunningTime() {
		return runningTime;
	}

	public String getAgeRestriction() {
		return ageRestriction;
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

	public void increaseFavoriteCount() {
		this.favoriteCount++;
	}

	public void decreaseFavoriteCount() {
		if (this.favoriteCount > 0) {
			this.favoriteCount--;
		}
	}

}

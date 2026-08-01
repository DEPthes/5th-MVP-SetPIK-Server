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
import java.time.LocalDateTime;

/** Flyway의 Ticket_Schedules 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Ticket_Schedules")
public class TicketSchedule extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ticket_schedule_id", nullable = false)
	private Long ticketScheduleId;

	@Column(name = "schedule_name", nullable = false, length = 255)
	private String scheduleName;

	@Enumerated(EnumType.STRING)
	@Column(name = "sale_type", nullable = false, length = 50)
	private TicketSaleType saleType;

	@Column(name = "opens_at", nullable = false)
	private LocalDateTime opensAt;

	@Column(name = "closes_at", nullable = true)
	private LocalDateTime closesAt;

	@Column(name = "booking_url", nullable = true, length = 2048)
	private String bookingUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "sale_status", nullable = false, length = 50)
	private SaleStatus saleStatus;

	@Column(name = "performance_id", nullable = false)
	private Long performanceId;

	protected TicketSchedule() {
	}

	public Long getTicketScheduleId() {
		return ticketScheduleId;
	}

	public String getScheduleName() {
		return scheduleName;
	}

	public TicketSaleType getSaleType() {
		return saleType;
	}

	public LocalDateTime getOpensAt() {
		return opensAt;
	}

	public LocalDateTime getClosesAt() {
		return closesAt;
	}

	public String getBookingUrl() {
		return bookingUrl;
	}

	public SaleStatus getSaleStatus() {
		return saleStatus;
	}

	public Long getPerformanceId() {
		return performanceId;
	}

}

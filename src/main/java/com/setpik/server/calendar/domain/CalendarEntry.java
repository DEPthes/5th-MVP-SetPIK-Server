package com.setpik.server.calendar.domain;

import com.setpik.server.common.domain.CreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Flyway의 Calendar_Entries 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Calendar_Entries")
public class CalendarEntry extends CreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "calendar_entry_id", nullable = false)
	private Long calendarEntryId;

	@Column(name = "calendar_at", nullable = false)
	private LocalDateTime calendarAt;

	@Column(name = "external_event_id", nullable = true, length = 255)
	private String externalEventId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "ticket_schedule_id", nullable = false)
	private Long ticketScheduleId;

	protected CalendarEntry() {
	}

	public Long getCalendarEntryId() {
		return calendarEntryId;
	}

	public LocalDateTime getCalendarAt() {
		return calendarAt;
	}

	public String getExternalEventId() {
		return externalEventId;
	}

	public Long getUserId() {
		return userId;
	}

	public Long getTicketScheduleId() {
		return ticketScheduleId;
	}

}

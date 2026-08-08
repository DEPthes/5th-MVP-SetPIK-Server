package com.setpik.server.performance.dto;

import com.setpik.server.performance.domain.TicketSchedule;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record TicketScheduleResponse(
	Long ticketScheduleId,
	String scheduleName,
	String saleType,
	OffsetDateTime opensAt,
	OffsetDateTime closesAt,
	String saleStatus
) {
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public static TicketScheduleResponse from(TicketSchedule ticketSchedule) {
		return new TicketScheduleResponse(
			ticketSchedule.getTicketScheduleId(),
			ticketSchedule.getScheduleName(),
			ticketSchedule.getSaleType().name(),
			ticketSchedule.getOpensAt() == null ? null : ticketSchedule.getOpensAt().atZone(KST).toOffsetDateTime(),
			ticketSchedule.getClosesAt() == null ? null : ticketSchedule.getClosesAt().atZone(KST).toOffsetDateTime(),
			ticketSchedule.getSaleStatus().name()
		);
	}
}

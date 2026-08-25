package com.setpik.server.performance.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PerformanceTest {

	@Test
	void computesMinTicketPriceFromTicketPriceTextOnCreate() {
		LocalDate date = LocalDate.of(2026, 8, 15);
		Performance performance = new Performance(
			"PF001", "공연", date, date.plusDays(1), null, null,
			PerformanceStatus.SCHEDULED, "PAID", "R석 100,000원 / S석 80,000원",
			LocalDateTime.now(), 1L);

		assertThat(performance.getMinTicketPrice()).isEqualTo(80000);
	}

	@Test
	void recomputesMinTicketPriceOnResync() {
		LocalDate date = LocalDate.of(2026, 8, 15);
		Performance performance = new Performance(
			"PF001", "공연", date, date.plusDays(1), null, null,
			PerformanceStatus.SCHEDULED, "PAID", "50,000원",
			LocalDateTime.now(), 1L);
		assertThat(performance.getMinTicketPrice()).isEqualTo(50000);

		performance.syncFromKopis("공연", date, date.plusDays(1), null, null,
			PerformanceStatus.ON_SALE, "FREE", "무료", LocalDateTime.now(), 1L);

		assertThat(performance.getMinTicketPrice()).isZero();
	}

	@Test
	void leavesMinTicketPriceNullWhenPriceUnknown() {
		LocalDate date = LocalDate.of(2026, 8, 15);
		Performance performance = new Performance(
			"PF001", "공연", date, date.plusDays(1), null, null,
			PerformanceStatus.SCHEDULED, "UNKNOWN", "미정",
			LocalDateTime.now(), 1L);

		assertThat(performance.getMinTicketPrice()).isNull();
	}
}

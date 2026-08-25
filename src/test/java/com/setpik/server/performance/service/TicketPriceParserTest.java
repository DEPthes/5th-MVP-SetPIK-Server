package com.setpik.server.performance.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TicketPriceParserTest {

	@Test
	void extractsLowestPriceFromMultiplePricedText() {
		assertThat(TicketPriceParser.parseMinPrice("PAID", "R석 100,000원 S석 80,000원")).isEqualTo(80000);
	}

	@Test
	void freePriceTypeReturnsZeroRegardlessOfText() {
		assertThat(TicketPriceParser.parseMinPrice("FREE", "전석 무료")).isEqualTo(0);
	}

	@Test
	void unknownPriceTypeReturnsNull() {
		assertThat(TicketPriceParser.parseMinPrice("UNKNOWN", "미정")).isNull();
	}

	@Test
	void paidPriceTypeWithoutDigitsInTextReturnsNullInsteadOfFailing() {
		assertThat(TicketPriceParser.parseMinPrice("PAID", "가격 미상")).isNull();
	}
}

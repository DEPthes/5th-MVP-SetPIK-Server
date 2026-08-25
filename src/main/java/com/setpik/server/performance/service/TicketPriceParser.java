package com.setpik.server.performance.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Performance에는 숫자 가격 필드가 없어 ticketPriceText(KOPIS 자유 텍스트)에서 최저가를 추출한다. */
public final class TicketPriceParser {

	private static final Pattern PRICE_PATTERN = Pattern.compile("([0-9][0-9,]*)\\s*원");

	private TicketPriceParser() {
	}

	public static Integer parseMinPrice(String priceType, String ticketPriceText) {
		if ("FREE".equals(priceType)) return 0;
		if (ticketPriceText == null || ticketPriceText.isBlank()) return null;

		Integer min = null;
		Matcher matcher = PRICE_PATTERN.matcher(ticketPriceText);
		while (matcher.find()) {
			int value = Integer.parseInt(matcher.group(1).replace(",", ""));
			if (min == null || value < min) min = value;
		}
		return min;
	}
}

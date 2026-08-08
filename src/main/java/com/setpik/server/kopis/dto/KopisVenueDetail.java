package com.setpik.server.kopis.dto;

import java.math.BigDecimal;

public record KopisVenueDetail(
	String facilityId,
	String venueName,
	String address,
	BigDecimal latitude,
	BigDecimal longitude
) {
}

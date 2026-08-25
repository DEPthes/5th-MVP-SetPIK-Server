package com.setpik.server.performance.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.setpik.server.auth.security.JwtAccessTokenProvider;
import com.setpik.server.common.api.PageResponse;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.performance.dto.PerformanceBrowseResponse;
import com.setpik.server.performance.dto.PerformanceDetailResponse;
import com.setpik.server.performance.dto.TicketScheduleResponse;
import com.setpik.server.performance.service.PerformanceService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PerformanceControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtAccessTokenProvider accessTokenProvider;

	@MockitoBean
	private PerformanceService performanceService;

	@Test
	void returnsPerformanceDetailWithoutAuthentication() throws Exception {
		when(performanceService.getPerformance(1001L)).thenReturn(new PerformanceDetailResponse(
			1001L,
			"2026 인천 펜타포트 록 페스티벌",
			LocalDate.of(2026, 8, 15),
			LocalDate.of(2026, 8, 17),
			"https://images.example.com/performances/1001.jpg",
			"https://tickets.example.com/performances/1001",
			"ON_SALE",
			new PerformanceDetailResponse.VenueResponse(77L, "송도달빛축제공원", "인천")
		));

		mockMvc.perform(get("/api/v1/performances/{performanceId}", 1001L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.result.performanceId").value(1001))
			.andExpect(jsonPath("$.result.performanceName")
				.value("2026 인천 펜타포트 록 페스티벌"))
			.andExpect(jsonPath("$.result.posterUrl")
				.value("https://images.example.com/performances/1001.jpg"))
			.andExpect(jsonPath("$.result.startDate").value("2026-08-15"))
			.andExpect(jsonPath("$.result.endDate").value("2026-08-17"))
			.andExpect(jsonPath("$.result.venue.venueId").value(77))
			.andExpect(jsonPath("$.result.venue.venueName").value("송도달빛축제공원"))
			.andExpect(jsonPath("$.result.venue.city").value("인천"))
			.andExpect(jsonPath("$.result.bookingUrl")
				.value("https://tickets.example.com/performances/1001"))
			.andExpect(jsonPath("$.result.performanceStatus").value("ON_SALE"))
			.andExpect(jsonPath("$.result.priceType").doesNotExist())
			.andExpect(jsonPath("$.result.favoriteCount").doesNotExist())
			.andExpect(jsonPath("$.result.venue.address").doesNotExist());
	}

	@Test
	void validatesPerformanceId() throws Exception {
		mockMvc.perform(get("/api/v1/performances/0"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));
	}

	@Test
	void returnsNotFoundForMissingPerformance() throws Exception {
		when(performanceService.getPerformance(999999L))
			.thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

		mockMvc.perform(get("/api/v1/performances/999999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(2003));
	}

	@Test
	void returnsTicketSchedulesWithoutAuthentication() throws Exception {
		when(performanceService.getTicketSchedules(1001L)).thenReturn(List.of(
			new TicketScheduleResponse(
				5001L,
				"선예매",
				"PRE_SALE",
				OffsetDateTime.parse("2026-08-01T20:00:00+09:00"),
				OffsetDateTime.parse("2026-08-01T23:59:59+09:00"),
				"SCHEDULED"
			)
		));

		mockMvc.perform(get("/api/v1/performances/{performanceId}/ticket-schedules", 1001L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.result[0].ticketScheduleId").value(5001))
			.andExpect(jsonPath("$.result[0].scheduleName").value("선예매"))
			.andExpect(jsonPath("$.result[0].saleType").value("PRE_SALE"))
			.andExpect(jsonPath("$.result[0].opensAt")
				.value("2026-08-01T20:00:00+09:00"))
			.andExpect(jsonPath("$.result[0].closesAt")
				.value("2026-08-01T23:59:59+09:00"))
			.andExpect(jsonPath("$.result[0].saleStatus").value("SCHEDULED"))
			.andExpect(jsonPath("$.result[0].bookingUrl").doesNotExist());
	}

	@Test
	void validatesTicketSchedulePerformanceId() throws Exception {
		mockMvc.perform(get("/api/v1/performances/0/ticket-schedules"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));
	}

	@Test
	void returnsBrowsedPerformancesUsingRequestedFiltersForAuthenticatedUser() throws Exception {
		when(performanceService.browsePerformances(
			eq(1L), eq("페스티벌"), eq("FESTIVAL"), eq("인천"),
			eq(LocalDate.of(2026, 8, 1)), eq(LocalDate.of(2026, 12, 31)),
			eq(0), eq(20), eq("recommended,desc")
		)).thenReturn(new PageResponse<>(
			List.of(new PerformanceBrowseResponse(
				1001L, "2026 인천 펜타포트 록 페스티벌",
				"https://images.example.com/performances/1001.jpg",
				LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 17),
				"송도달빛축제공원", "인천", List.of("Artist A"),
				"FESTIVAL", "ON_SALE", 80000, 2003
			)),
			0, 20, 1, 1, false
		));

		mockMvc.perform(get("/api/v1/performances")
				.param("keyword", "페스티벌")
				.param("performanceType", "FESTIVAL")
				.param("region", "인천")
				.param("fromDate", "2026-08-01")
				.param("toDate", "2026-12-31")
				.param("sort", "recommended,desc")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(1L)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.result.content[0].performanceId").value(1001))
			.andExpect(jsonPath("$.result.content[0].performanceName")
				.value("2026 인천 펜타포트 록 페스티벌"))
			.andExpect(jsonPath("$.result.content[0].venueName").value("송도달빛축제공원"))
			.andExpect(jsonPath("$.result.content[0].region").value("인천"))
			.andExpect(jsonPath("$.result.content[0].performanceType").value("FESTIVAL"))
			.andExpect(jsonPath("$.result.content[0].minTicketPrice").value(80000))
			.andExpect(jsonPath("$.result.content[0].recommendationScore").value(2003))
			.andExpect(jsonPath("$.result.totalElements").value(1));
	}

	@Test
	void rejectsBrowsingWithoutBearerToken() throws Exception {
		mockMvc.perform(get("/api/v1/performances"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(2001));
	}

	@Test
	void appliesDefaultPagingAndSortWhenOmitted() throws Exception {
		when(performanceService.browsePerformances(
			eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(),
			eq(0), eq(20), eq("recommended,desc")
		)).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, false));

		mockMvc.perform(get("/api/v1/performances")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(1L)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.result.totalElements").value(0));
	}

	private String bearerToken(Long userId) {
		return "Bearer " + accessTokenProvider.issue(userId);
	}
}

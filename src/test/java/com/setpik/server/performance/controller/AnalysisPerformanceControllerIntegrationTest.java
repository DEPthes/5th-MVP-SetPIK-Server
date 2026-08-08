package com.setpik.server.performance.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.setpik.server.auth.security.JwtAccessTokenProvider;
import com.setpik.server.performance.dto.PerformanceMatchRequest;
import com.setpik.server.performance.dto.PerformanceMatchResponse;
import com.setpik.server.performance.dto.PerformanceRecommendationResponse;
import com.setpik.server.performance.dto.MatchedArtistResponse;
import com.setpik.server.common.api.PageResponse;
import com.setpik.server.performance.service.PerformanceMatchingService;
import com.setpik.server.performance.service.PerformanceService;
import java.util.List;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalysisPerformanceControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtAccessTokenProvider accessTokenProvider;

	@MockitoBean
	private PerformanceMatchingService performanceMatchingService;

	@MockitoBean
	private PerformanceService performanceService;

	@Test
	void calculatesPerformanceMatchesUsingAuthenticatedUser() throws Exception {
		PerformanceMatchRequest request = new PerformanceMatchRequest(
			java.time.LocalDate.of(2026, 8, 1),
			java.time.LocalDate.of(2026, 12, 31)
		);
		when(performanceMatchingService.calculate(eq(1L), eq(501L), eq(request)))
			.thenReturn(new PerformanceMatchResponse(
				501L,
				12,
				OffsetDateTime.parse("2026-07-28T10:50:00+09:00")
			));

		mockMvc.perform(post("/api/v1/analyses/{analysisId}/matches", 501L)
				.header(HttpHeaders.AUTHORIZATION, bearerToken(1L))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "fromDate": "2026-08-01",
					  "toDate": "2026-12-31"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value(1100))
			.andExpect(jsonPath("$.message").value("공연 매칭 계산이 완료되었습니다."))
			.andExpect(jsonPath("$.result.analysisId").value(501))
			.andExpect(jsonPath("$.result.matchedPerformanceCount").value(12))
			.andExpect(jsonPath("$.result.calculatedAt").value("2026-07-28T10:50:00+09:00"));
	}

	@Test
	void rejectsRequestWithoutBearerToken() throws Exception {
		mockMvc.perform(post("/api/v1/analyses/{analysisId}/matches", 501L)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(2001));
	}

	@Test
	void returnsPagedRecommendationsUsingRequestedSort() throws Exception {
		PageResponse<PerformanceRecommendationResponse> result = new PageResponse<>(
			List.of(new PerformanceRecommendationResponse(
				900L, 1001L, "2026 인천 펜타포트 록 페스티벌",
				(byte) 1, 2, (byte) 40,
				"플레이리스트 아티스트 2명이 직접 출연합니다."
			)),
			0, 20, 1, 1, false
		);
		when(performanceService.getRecommendedPerformances(
			eq(1L), eq(501L), eq(0), eq(20), eq("matchPriority,asc")))
			.thenReturn(result);

		mockMvc.perform(get("/api/v1/analyses/{analysisId}/performances", 501L)
				.param("page", "0")
				.param("size", "20")
				.param("sort", "matchPriority,asc")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(1L)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.result.content[0].matchId").value(900))
			.andExpect(jsonPath("$.result.content[0].performanceId").value(1001))
			.andExpect(jsonPath("$.result.content[0].performanceName")
				.value("2026 인천 펜타포트 록 페스티벌"))
			.andExpect(jsonPath("$.result.content[0].matchPriority").value(1))
			.andExpect(jsonPath("$.result.content[0].matchedArtistCount").value(2))
			.andExpect(jsonPath("$.result.content[0].matchRatio").value(40))
			.andExpect(jsonPath("$.result.page").value(0))
			.andExpect(jsonPath("$.result.size").value(20))
			.andExpect(jsonPath("$.result.totalElements").value(1))
			.andExpect(jsonPath("$.result.totalPages").value(1))
			.andExpect(jsonPath("$.result.hasNext").value(false));

		verify(performanceService).getRecommendedPerformances(
			1L, 501L, 0, 20, "matchPriority,asc");
	}

	@Test
	void returnsMatchedArtistsForOwnedAnalysis() throws Exception {
		when(performanceService.getMatchedArtists(1L, 501L, 1001L)).thenReturn(List.of(
			new MatchedArtistResponse(7L, "Artist A", 6, true)
		));

		mockMvc.perform(get(
				"/api/v1/analyses/{analysisId}/performances/{performanceId}/matched-artists",
				501L,
				1001L
			)
			.header(HttpHeaders.AUTHORIZATION, bearerToken(1L)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.result[0].artistId").value(7))
			.andExpect(jsonPath("$.result[0].artistName").value("Artist A"))
			.andExpect(jsonPath("$.result[0].occurrenceCount").value(6))
			.andExpect(jsonPath("$.result[0].isHeadliner").value(true));
	}

	@Test
	void validatesMatchedArtistPathIdsAndAuthentication() throws Exception {
		String path = "/api/v1/analyses/{analysisId}/performances/{performanceId}/matched-artists";

		mockMvc.perform(get(path, 0L, 1001L)
				.header(HttpHeaders.AUTHORIZATION, bearerToken(1L)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));

		mockMvc.perform(get(path, 501L, 1001L))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(2001));
	}

	private String bearerToken(Long userId) {
		return "Bearer " + accessTokenProvider.issue(userId);
	}
}

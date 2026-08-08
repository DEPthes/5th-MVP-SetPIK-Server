package com.setpik.server.kopis.controller;

import com.setpik.server.common.api.ApiResponse;
import com.setpik.server.kopis.dto.KopisSyncResponse;
import com.setpik.server.kopis.service.KopisPerformanceSyncService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/kopis")
@Tag(name = "KOPIS 내부 연동", description = "운영자가 KOPIS 공연 데이터를 내부 DB로 동기화하는 API")
public class KopisSyncController {

	private final KopisPerformanceSyncService syncService;

	public KopisSyncController(KopisPerformanceSyncService syncService) {
		this.syncService = syncService;
	}

	@PostMapping("/performances/sync")
	public ApiResponse<KopisSyncResponse> syncPerformances(
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
	) {
		return ApiResponse.success("KOPIS 공연 동기화가 완료되었습니다.",
			syncService.sync(fromDate, toDate));
	}
}

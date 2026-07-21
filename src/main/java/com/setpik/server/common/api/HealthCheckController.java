package com.setpik.server.common.api;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthCheckController {

	@GetMapping("/health")
	public Map<String, Object> health() {
		return Map.of(
			"status", "UP",
			"service", "setpik-server",
			"timestamp", Instant.now().toString()
		);
	}
}

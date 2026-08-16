package com.setpik.server.kopis.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.setpik.server.kopis.config.KopisApiProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class KopisClientTest {

	@Test
	void retriesTemporaryServerFailure() throws Exception {
		AtomicInteger requestCount = new AtomicInteger();
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/pblprfr", exchange -> {
			int attempt = requestCount.incrementAndGet();
			if (attempt == 1) {
				exchange.sendResponseHeaders(500, -1);
			} else {
				byte[] body = "<dbs><db><mt20id>PF001</mt20id></db></dbs>"
					.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().add("Content-Type", "application/xml;charset=UTF-8");
				exchange.sendResponseHeaders(200, body.length);
				exchange.getResponseBody().write(body);
			}
			exchange.close();
		});
		server.start();

		try {
			KopisApiProperties properties = new KopisApiProperties();
			properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
			properties.setServiceKey("test-key");
			properties.setRetryMaxAttempts(2);
			properties.setRetryDelay(Duration.ZERO);
			properties.setConnectTimeout(Duration.ofSeconds(1));
			properties.setReadTimeout(Duration.ofSeconds(1));
			KopisClient client = new KopisClient(
				properties, new KopisXmlParser(), RestClient.builder());

			List<String> ids = client.getPerformanceIds(
				LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 15), 1, 100);

			assertThat(ids).containsExactly("PF001");
			assertThat(requestCount).hasValue(2);
		} finally {
			server.stop(0);
		}
	}
}

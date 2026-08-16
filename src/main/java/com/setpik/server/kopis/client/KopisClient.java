package com.setpik.server.kopis.client;

import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.kopis.config.KopisApiProperties;
import com.setpik.server.kopis.dto.KopisPerformanceDetail;
import com.setpik.server.kopis.dto.KopisVenueDetail;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

@Component
public class KopisClient {

	private static final Logger log = LoggerFactory.getLogger(KopisClient.class);
	private static final DateTimeFormatter QUERY_DATE = DateTimeFormatter.BASIC_ISO_DATE;
	private final KopisApiProperties properties;
	private final KopisXmlParser parser;
	private final RestClient restClient;

	public KopisClient(KopisApiProperties properties, KopisXmlParser parser, RestClient.Builder builder) {
		this.properties = properties;
		this.parser = parser;
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.getConnectTimeout());
		requestFactory.setReadTimeout(properties.getReadTimeout());
		this.restClient = builder
			.baseUrl(properties.getBaseUrl())
			.requestFactory(requestFactory)
			.build();
	}

	public List<String> getPerformanceIds(LocalDate fromDate, LocalDate toDate, int page, int rows) {
		String xml = get("/pblprfr", fromDate, toDate, page, rows);
		return parser.performanceIds(xml);
	}

	public KopisPerformanceDetail getPerformanceDetail(String performanceId) {
		String xml = get("/pblprfr/" + performanceId, null, null, null, null);
		return parser.performanceDetail(xml);
	}

	public KopisVenueDetail getVenueDetail(String facilityId) {
		String xml = get("/prfplc/" + facilityId, null, null, null, null);
		return parser.venueDetail(xml);
	}

	private String get(String path, LocalDate fromDate, LocalDate toDate,
		Integer page, Integer rows) {
		if (properties.getServiceKey() == null || properties.getServiceKey().isBlank()) {
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
		int maxAttempts = Math.max(1, properties.getRetryMaxAttempts());
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				return request(path, fromDate, toDate, page, rows);
			} catch (RestClientException exception) {
				if (!isRetryable(exception) || attempt == maxAttempts) {
					throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
				}
				log.warn("KOPIS API 호출 재시도: path={}, attempt={}/{}",
					path, attempt, maxAttempts);
				waitBeforeRetry(attempt);
			}
		}
		throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
	}

	private String request(String path, LocalDate fromDate, LocalDate toDate,
		Integer page, Integer rows) {
		return restClient.get()
			.uri(builder -> {
				builder.path(path).queryParam("service", properties.getServiceKey());
				if (fromDate != null) builder.queryParam("stdate", fromDate.format(QUERY_DATE));
				if (toDate != null) builder.queryParam("eddate", toDate.format(QUERY_DATE));
				if (page != null) builder.queryParam("cpage", page);
				if (rows != null) builder.queryParam("rows", rows);
				return builder.build();
			})
			.retrieve()
			.body(String.class);
	}

	private boolean isRetryable(RestClientException exception) {
		if (exception instanceof ResourceAccessException) {
			return true;
		}
		if (exception instanceof RestClientResponseException responseException) {
			int status = responseException.getStatusCode().value();
			return status == 429 || status >= 500;
		}
		return false;
	}

	private void waitBeforeRetry(int attempt) {
		long delayMillis = properties.getRetryDelay().toMillis() * attempt;
		try {
			Thread.sleep(delayMillis);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}
}

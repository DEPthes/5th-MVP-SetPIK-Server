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
	private static final int MAX_ERROR_RESPONSE_LENGTH = 500;
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
					logFinalFailure(path, attempt, exception);
					throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
				}
				log.warn("KOPIS API 호출 재시도: path={}, status={}, attempt={}/{}",
					path, getStatus(exception), attempt, maxAttempts);
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
			// KOPIS가 동일한 정상 GET 요청에 간헐적으로 400을 반환한 뒤 재호출 시 복구된다.
			return status == 400 || status == 408 || status == 429 || status >= 500;
		}
		return false;
	}

	private String getStatus(RestClientException exception) {
		if (exception instanceof RestClientResponseException responseException) {
			return String.valueOf(responseException.getStatusCode().value());
		}
		return exception.getClass().getSimpleName();
	}

	private void logFinalFailure(String path, int attempts, RestClientException exception) {
		if (exception instanceof RestClientResponseException responseException) {
			String responseBody = responseException.getResponseBodyAsString();
			if (responseBody.length() > MAX_ERROR_RESPONSE_LENGTH) {
				responseBody = responseBody.substring(0, MAX_ERROR_RESPONSE_LENGTH);
			}
			log.error("KOPIS API 호출 최종 실패: path={}, status={}, attempts={}, response={}",
				path, responseException.getStatusCode().value(), attempts, responseBody);
			return;
		}
		log.error("KOPIS API 호출 최종 실패: path={}, attempts={}", path, attempts, exception);
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

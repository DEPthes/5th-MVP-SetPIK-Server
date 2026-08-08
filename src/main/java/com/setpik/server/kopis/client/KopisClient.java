package com.setpik.server.kopis.client;

import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.kopis.config.KopisApiProperties;
import com.setpik.server.kopis.dto.KopisPerformanceDetail;
import com.setpik.server.kopis.dto.KopisVenueDetail;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KopisClient {

	private static final DateTimeFormatter QUERY_DATE = DateTimeFormatter.BASIC_ISO_DATE;
	private final KopisApiProperties properties;
	private final KopisXmlParser parser;
	private final RestClient restClient;

	public KopisClient(KopisApiProperties properties, KopisXmlParser parser, RestClient.Builder builder) {
		this.properties = properties;
		this.parser = parser;
		this.restClient = builder.baseUrl(properties.getBaseUrl()).build();
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
		try {
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
		} catch (RestClientException exception) {
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}
}

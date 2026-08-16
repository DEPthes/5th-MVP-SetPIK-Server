package com.setpik.server.kopis.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "kopis.api")
public class KopisApiProperties {

	private String baseUrl;
	private String serviceKey;
	private int detailConcurrency = 5;
	private int batchSize = 50;
	private int retryMaxAttempts = 3;
	private Duration retryDelay = Duration.ofMillis(500);
	private Duration connectTimeout = Duration.ofSeconds(3);
	private Duration readTimeout = Duration.ofSeconds(10);

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getServiceKey() {
		return serviceKey;
	}

	public void setServiceKey(String serviceKey) {
		this.serviceKey = serviceKey;
	}

	public int getDetailConcurrency() {
		return detailConcurrency;
	}

	public void setDetailConcurrency(int detailConcurrency) {
		this.detailConcurrency = detailConcurrency;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public int getRetryMaxAttempts() {
		return retryMaxAttempts;
	}

	public void setRetryMaxAttempts(int retryMaxAttempts) {
		this.retryMaxAttempts = retryMaxAttempts;
	}

	public Duration getRetryDelay() {
		return retryDelay;
	}

	public void setRetryDelay(Duration retryDelay) {
		this.retryDelay = retryDelay;
	}

	public Duration getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public Duration getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(Duration readTimeout) {
		this.readTimeout = readTimeout;
	}
}

package com.setpik.server.kopis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "kopis.sync")
public class KopisSyncProperties {

	private int futureDays = 1;

	public int getFutureDays() {
		return futureDays;
	}

	public void setFutureDays(int futureDays) {
		this.futureDays = futureDays;
	}
}

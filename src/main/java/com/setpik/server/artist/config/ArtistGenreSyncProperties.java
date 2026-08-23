package com.setpik.server.artist.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "artist-genre.sync")
public class ArtistGenreSyncProperties {
	private int batchSize = 20;

	public int getBatchSize() { return batchSize; }
	public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
}

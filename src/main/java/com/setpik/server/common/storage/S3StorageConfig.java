package com.setpik.server.common.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(S3StorageProperties.class)
public class S3StorageConfig {

	@Bean
	@Profile("prod")
	S3Client s3Client(S3StorageProperties properties) {
		return S3Client.builder()
			.region(Region.of(properties.region()))
			.build();
	}
}

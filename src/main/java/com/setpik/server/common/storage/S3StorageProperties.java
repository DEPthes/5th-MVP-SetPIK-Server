package com.setpik.server.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.s3")
public record S3StorageProperties(
	String region,
	String bucketName,
	String baseUrl,
	String defaultProfileUrl
) {
}

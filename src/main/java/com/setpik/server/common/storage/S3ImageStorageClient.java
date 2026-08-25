package com.setpik.server.common.storage;

import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@Profile("prod")
public class S3ImageStorageClient implements ImageStorageClient {

	private static final String PROFILE_IMAGE_PREFIX = "profile-images/";
	private static final Map<String, String> EXTENSIONS = Map.of(
		"image/jpeg", "jpg",
		"image/png", "png",
		"image/webp", "webp"
	);

	private final S3Client s3Client;
	private final S3StorageProperties properties;

	public S3ImageStorageClient(S3Client s3Client, S3StorageProperties properties) {
		this.s3Client = s3Client;
		this.properties = properties;
	}

	@Override
	public String upload(Long userId, MultipartFile file) {
		String extension = EXTENSIONS.get(file.getContentType());
		if (extension == null) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		String key = PROFILE_IMAGE_PREFIX + userId + "/" + UUID.randomUUID() + "." + extension;
		PutObjectRequest request = PutObjectRequest.builder()
			.bucket(properties.bucketName())
			.key(key)
			.contentType(file.getContentType())
			.cacheControl("public, max-age=31536000, immutable")
			.build();

		try {
			s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
			return normalizedBaseUrl() + "/" + key;
		} catch (IOException | RuntimeException exception) {
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public void delete(String imageUrl) {
		if (imageUrl == null) {
			return;
		}
		String prefix = normalizedBaseUrl() + "/" + PROFILE_IMAGE_PREFIX;
		if (!imageUrl.startsWith(prefix)) {
			return;
		}

		String key = imageUrl.substring(normalizedBaseUrl().length() + 1);
		try {
			s3Client.deleteObject(DeleteObjectRequest.builder()
				.bucket(properties.bucketName())
				.key(key)
				.build());
		} catch (RuntimeException exception) {
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	private String normalizedBaseUrl() {
		return properties.baseUrl().replaceAll("/+$", "");
	}
}

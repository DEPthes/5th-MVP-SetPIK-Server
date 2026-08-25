package com.setpik.server.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class S3ImageStorageClientTest {

	private S3Client s3Client;
	private S3ImageStorageClient storageClient;

	@BeforeEach
	void setUp() {
		s3Client = mock(S3Client.class);
		S3StorageProperties properties = new S3StorageProperties(
			"ap-northeast-2",
			"setpik-profile-images",
			"https://d23ywix9e5rzc2.cloudfront.net/",
			"https://d23ywix9e5rzc2.cloudfront.net/default-profile.png");
		storageClient = new S3ImageStorageClient(s3Client, properties);
	}

	@Test
	void uploadsImageUnderUserSpecificPrefixAndReturnsCloudFrontUrl() {
		when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
			.thenReturn(PutObjectResponse.builder().build());
		MockMultipartFile file = new MockMultipartFile(
			"image", "profile.png", "image/png", "image".getBytes());

		String url = storageClient.upload(42L, file);

		assertThat(url).startsWith(
			"https://d23ywix9e5rzc2.cloudfront.net/profile-images/42/");
		assertThat(url).endsWith(".png");
		verify(s3Client).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
	}

	@Test
	void deletesOnlyManagedProfileImageUrl() {
		when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
			.thenReturn(DeleteObjectResponse.builder().build());

		storageClient.delete(
			"https://d23ywix9e5rzc2.cloudfront.net/profile-images/42/existing.png");
		storageClient.delete(
			"https://d23ywix9e5rzc2.cloudfront.net/default-profile.png");

		verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	void ignoresExternalImageUrlOnDelete() {
		storageClient.delete("https://i.scdn.co/image/spotify-image");

		verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
	}
}

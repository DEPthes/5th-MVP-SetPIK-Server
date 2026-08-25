package com.setpik.server.common.storage;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * S3 연동 전까지 사용하는 임시 구현체로, 실제 저장 없이 가짜 URL만 반환한다.
 * TODO: S3 연동이 끝나면 이 구현체를 S3ImageStorageClient로 교체한다.
 */
@Component
public class NoOpImageStorageClient implements ImageStorageClient {

	private static final String PLACEHOLDER_BASE_URL = "https://placeholder.setpik.local/profile-images/";

	@Override
	public String upload(MultipartFile file) {
		return PLACEHOLDER_BASE_URL + UUID.randomUUID();
	}
}

package com.setpik.server.common.storage;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * S3 연동 전까지 사용하는 임시 구현체로, 실제 저장 없이 가짜 URL만 반환한다.
 * TODO: S3 연동이 끝나면 이 구현체를 S3ImageStorageClient로 교체한다.
 */
@Component
@Profile("!prod")
public class NoOpImageStorageClient implements ImageStorageClient {

	private static final String PLACEHOLDER_BASE_URL = "https://placeholder.setpik.local/profile-images/";

	@Override
	public String upload(Long userId, MultipartFile file) {
		return PLACEHOLDER_BASE_URL + userId + "/" + UUID.randomUUID();
	}

	@Override
	public void delete(String imageUrl) {
		// 로컬/테스트 프로필에서는 외부 저장소를 사용하지 않는다.
	}
}

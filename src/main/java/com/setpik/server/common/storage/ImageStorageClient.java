package com.setpik.server.common.storage;

import org.springframework.web.multipart.MultipartFile;

/** 이미지 파일을 외부 스토리지(S3 등)에 업로드하고 공개 접근 가능한 URL을 반환한다. */
public interface ImageStorageClient {

	String upload(MultipartFile file);
}

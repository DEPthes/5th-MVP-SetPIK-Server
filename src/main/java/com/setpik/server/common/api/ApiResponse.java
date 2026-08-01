package com.setpik.server.common.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.setpik.server.common.exception.ErrorCode;

/** API 명세서의 공통 응답 형식을 모든 Controller에서 재사용한다. */
public record ApiResponse<T>(
	@JsonProperty("isSuccess") boolean isSuccess,
	int code,
	String message,
	T result
) {

	public static <T> ApiResponse<T> success(T result) {
		return new ApiResponse<>(true, 1000, "요청에 성공했습니다.", result);
	}

	public static <T> ApiResponse<T> success(String message, T result) {
		return new ApiResponse<>(true, 1000, message, result);
	}

	public static <T> ApiResponse<T> created(String message, T result) {
		return new ApiResponse<>(true, 1100, message, result);
	}

	public static <T> ApiResponse<T> failure(ErrorCode errorCode, T result) {
		return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), result);
	}
}

package com.setpik.server.common.api;

import org.springframework.data.domain.Page;
import java.util.List;

/** 목록 조회 API의 공통 페이지네이션 응답. */
public record PageResponse<T>(
	List<T> content,
	int page,
	int size,
	long totalElements,
	int totalPages
) {
	public static <T> PageResponse<T> of(List<T> content, Page<?> page) {
		return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
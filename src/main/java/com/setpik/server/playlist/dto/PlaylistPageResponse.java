package com.setpik.server.playlist.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PlaylistPageResponse(
	List<PlaylistSummaryResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext
) {
	public static PlaylistPageResponse from(Page<PlaylistSummaryResponse> result) {
		return new PlaylistPageResponse(
			result.getContent(),
			result.getNumber(),
			result.getSize(),
			result.getTotalElements(),
			result.getTotalPages(),
			result.hasNext()
		);
	}
}

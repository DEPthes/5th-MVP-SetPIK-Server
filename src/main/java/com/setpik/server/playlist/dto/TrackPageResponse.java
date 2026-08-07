package com.setpik.server.playlist.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record TrackPageResponse(
	List<TrackResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext
) {
	public static TrackPageResponse from(Page<TrackResponse> result) {
		return new TrackPageResponse(
			result.getContent(), result.getNumber(), result.getSize(),
			result.getTotalElements(), result.getTotalPages(), result.hasNext()
		);
	}
}

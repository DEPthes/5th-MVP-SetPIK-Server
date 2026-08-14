package com.setpik.server.analysis.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record AnalysisArtistUpdateRequest(
	@NotEmpty(message = "수정할 아티스트 목록은 비어 있을 수 없습니다.")
	List<@Valid ArtistExclusion> artists
) {
	public record ArtistExclusion(
		@NotNull(message = "artistId는 필수입니다.")
		@Positive(message = "artistId는 양수여야 합니다.")
		Long artistId,

		@NotNull(message = "isExcluded는 필수입니다.")
		Boolean isExcluded
	) {
	}
}

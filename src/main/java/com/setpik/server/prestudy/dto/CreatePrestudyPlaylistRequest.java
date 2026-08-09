package com.setpik.server.prestudy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreatePrestudyPlaylistRequest(
	@NotBlank String playlistTitle,
	@NotNull Boolean isPublic,
	@NotNull Long analysisId,
	@NotEmpty List<Long> selectedTrackIds
) {
}
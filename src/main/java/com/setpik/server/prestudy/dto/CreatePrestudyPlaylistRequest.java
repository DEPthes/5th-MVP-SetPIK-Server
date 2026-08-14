package com.setpik.server.prestudy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePrestudyPlaylistRequest(
	@NotBlank @Size(max = 255) String playlistTitle,
	@NotNull Boolean isPublic,
	@NotNull @Positive Long analysisId,
	@NotEmpty @Size(max = 100) List<@NotNull @Positive Long> selectedTrackIds
) {
}

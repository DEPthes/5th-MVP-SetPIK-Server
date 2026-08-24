package com.setpik.server.member.dto;

import com.setpik.server.member.domain.OnboardingStatus;

public record OnboardingStatusResponse(
	OnboardingStatus status,
	Long selectedPlaylistId,
	Long analysisId
) {
}

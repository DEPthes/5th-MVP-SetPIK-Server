package com.setpik.server.member.service;

import com.setpik.server.analysis.domain.PlaylistAnalysis;
import com.setpik.server.analysis.repository.PlaylistAnalysisRepository;
import com.setpik.server.member.domain.OnboardingStatus;
import com.setpik.server.member.dto.OnboardingStatusResponse;
import com.setpik.server.playlist.domain.PlaylistRecentSelection;
import com.setpik.server.playlist.repository.PlaylistRecentSelectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OnboardingService {

	private final PlaylistRecentSelectionRepository recentSelectionRepository;
	private final PlaylistAnalysisRepository analysisRepository;

	public OnboardingService(
		PlaylistRecentSelectionRepository recentSelectionRepository,
		PlaylistAnalysisRepository analysisRepository
	) {
		this.recentSelectionRepository = recentSelectionRepository;
		this.analysisRepository = analysisRepository;
	}

	public OnboardingStatusResponse getStatus(Long userId) {
		PlaylistRecentSelection selection = recentSelectionRepository
			.findFirstByUserIdOrderBySelectedAtDescPlaylistIdDesc(userId)
			.orElse(null);
		if (selection == null) {
			return new OnboardingStatusResponse(OnboardingStatus.NOT_STARTED, null, null);
		}

		PlaylistAnalysis analysis = analysisRepository
			.findFirstByPlaylistIdAndUserIdOrderByAnalyzedAtDescAnalysisIdDesc(
				selection.getPlaylistId(), userId)
			.orElse(null);
		if (analysis == null) {
			return new OnboardingStatusResponse(
				OnboardingStatus.PLAYLIST_SELECTED, selection.getPlaylistId(), null);
		}

		OnboardingStatus status = analysis.getArtistSelectionCompletedAt() == null
			? OnboardingStatus.PLAYLIST_SELECTED
			: OnboardingStatus.COMPLETED;
		return new OnboardingStatusResponse(
			status, selection.getPlaylistId(), analysis.getAnalysisId());
	}
}

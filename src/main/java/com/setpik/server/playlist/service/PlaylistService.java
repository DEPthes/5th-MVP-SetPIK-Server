package com.setpik.server.playlist.service;

import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.playlist.domain.PlaylistTrack;
import com.setpik.server.playlist.domain.SpotifyPlaylist;
import com.setpik.server.playlist.domain.Track;
import com.setpik.server.playlist.dto.PlaylistDetailResponse;
import com.setpik.server.playlist.dto.PlaylistSummaryResponse;
import com.setpik.server.playlist.dto.TrackResponse;
import com.setpik.server.playlist.repository.PlaylistTrackRepository;
import com.setpik.server.playlist.repository.SpotifyPlaylistRepository;
import com.setpik.server.playlist.repository.TrackRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlaylistService {

	private final SpotifyPlaylistRepository playlistRepository;
	private final PlaylistTrackRepository playlistTrackRepository;
	private final TrackRepository trackRepository;

	public PlaylistService(SpotifyPlaylistRepository playlistRepository,
						   PlaylistTrackRepository playlistTrackRepository,
						   TrackRepository trackRepository) {
		this.playlistRepository = playlistRepository;
		this.playlistTrackRepository = playlistTrackRepository;
		this.trackRepository = trackRepository;
	}

	public List<PlaylistSummaryResponse> getMyPlaylists(Long userId) {
		return playlistRepository.findByUserIdAndDeletedAtIsNull(userId).stream()
			.map(PlaylistSummaryResponse::from)
			.toList();
	}

	public PlaylistDetailResponse getPlaylistDetail(Long userId, Long playlistId) {
		SpotifyPlaylist playlist = findOwnedPlaylist(userId, playlistId);
		return PlaylistDetailResponse.from(playlist);
	}

	public List<TrackResponse> getPlaylistTracks(Long userId, Long playlistId) {
		findOwnedPlaylist(userId, playlistId);

		List<PlaylistTrack> playlistTracks =
			playlistTrackRepository.findByPlaylistIdOrderByTrackPositionAsc(playlistId);

		List<Long> trackIds = playlistTracks.stream()
			.map(PlaylistTrack::getTrackId)
			.toList();

		Map<Long, Track> trackMap = trackRepository.findAllById(trackIds).stream()
			.collect(Collectors.toMap(Track::getTrackId, Function.identity()));

		return playlistTracks.stream()
			.map(pt -> {
				Track track = trackMap.get(pt.getTrackId());
				if (track == null) {
					throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
				}
				return TrackResponse.of(track, pt.getTrackPosition());
			})
			.toList();
	}

	private SpotifyPlaylist findOwnedPlaylist(Long userId, Long playlistId) {
		SpotifyPlaylist playlist = playlistRepository
			.findByPlaylistIdAndDeletedAtIsNull(playlistId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

		if (!playlist.getUserId().equals(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		return playlist;
	}
}

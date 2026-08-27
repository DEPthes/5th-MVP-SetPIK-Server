package com.setpik.server.prestudy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.setpik.server.analysis.domain.AnalysisStatus;
import com.setpik.server.analysis.domain.PlaylistAnalysis;
import com.setpik.server.analysis.repository.PlaylistAnalysisRepository;
import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.domain.ArtistAlias;
import com.setpik.server.artist.repository.ArtistAliasRepository;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.auth.client.SpotifyOAuthClient;
import com.setpik.server.auth.security.TokenCipher;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.performance.domain.Performance;
import com.setpik.server.performance.domain.PerformanceArtist;
import com.setpik.server.performance.domain.PerformanceMatch;
import com.setpik.server.performance.domain.PerformanceMatchArtist;
import com.setpik.server.performance.repository.PerformanceArtistRepository;
import com.setpik.server.performance.repository.PerformanceMatchArtistRepository;
import com.setpik.server.performance.repository.PerformanceMatchRepository;
import com.setpik.server.performance.repository.PerformanceRepository;
import com.setpik.server.playlist.client.SpotifyPlaylistClient;
import com.setpik.server.playlist.client.SpotifyPlaylistApiException;
import com.setpik.server.playlist.client.dto.SpotifyTrackSnapshot;
import com.setpik.server.playlist.domain.PlaylistTrack;
import com.setpik.server.playlist.domain.Track;
import com.setpik.server.playlist.domain.TrackArtist;
import com.setpik.server.playlist.repository.PlaylistTrackRepository;
import com.setpik.server.playlist.repository.TrackArtistRepository;
import com.setpik.server.playlist.repository.TrackRepository;
import com.setpik.server.prestudy.domain.PrestudyPlaylist;
import com.setpik.server.prestudy.domain.PrestudyPlaylistTrack;
import com.setpik.server.prestudy.domain.SourceType;
import com.setpik.server.prestudy.dto.CreatePrestudyPlaylistRequest;
import com.setpik.server.prestudy.dto.PrestudyPlaylistTrackResponse;
import com.setpik.server.prestudy.repository.PrestudyPlaylistRepository;
import com.setpik.server.prestudy.repository.PrestudyPlaylistTrackRepository;
import com.setpik.server.spotify.domain.ConnectionStatus;
import com.setpik.server.spotify.domain.SpotifyAccount;
import com.setpik.server.spotify.repository.SpotifyAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrestudyPlaylistServiceTest {

	@Mock private PrestudyPlaylistRepository prestudyPlaylistRepository;
	@Mock private PrestudyPlaylistTrackRepository prestudyPlaylistTrackRepository;
	@Mock private PerformanceRepository performanceRepository;
	@Mock private PerformanceArtistRepository performanceArtistRepository;
	@Mock private PerformanceMatchRepository performanceMatchRepository;
	@Mock private PerformanceMatchArtistRepository performanceMatchArtistRepository;
	@Mock private PlaylistAnalysisRepository playlistAnalysisRepository;
	@Mock private PlaylistTrackRepository playlistTrackRepository;
	@Mock private TrackRepository trackRepository;
	@Mock private ArtistRepository artistRepository;
	@Mock private ArtistAliasRepository artistAliasRepository;
	@Mock private TrackArtistRepository trackArtistRepository;
	@Mock private SpotifyAccountRepository spotifyAccountRepository;
	@Mock private SpotifyPlaylistClient spotifyPlaylistClient;
	@Mock private SpotifyOAuthClient spotifyOAuthClient;
	@Mock private TokenCipher tokenCipher;

	private PrestudyPlaylistService service;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(
			Instant.parse("2026-08-14T10:00:00Z"), ZoneId.of("Asia/Seoul"));
		service = new PrestudyPlaylistService(
			prestudyPlaylistRepository, prestudyPlaylistTrackRepository,
			performanceRepository, performanceArtistRepository, performanceMatchRepository,
			performanceMatchArtistRepository,
			playlistAnalysisRepository, playlistTrackRepository, trackRepository,
			artistRepository, artistAliasRepository, trackArtistRepository, spotifyAccountRepository,
			spotifyPlaylistClient, spotifyOAuthClient, tokenCipher, clock);
	}

	@Test
	void returnsTrackDisplayMetadataAndOrderedArtistNames() {
		PrestudyPlaylist playlist = mock(PrestudyPlaylist.class);
		PrestudyPlaylistTrack playlistTrack = new PrestudyPlaylistTrack(
			701L, 4001L, 1, SourceType.ORIGINAL_PLAYLIST, false);
		Track track = mock(Track.class);
		Artist firstArtist = mock(Artist.class);
		Artist secondArtist = mock(Artist.class);

		when(track.getTrackId()).thenReturn(4001L);
		when(track.getTrackName()).thenReturn("Song A");
		when(track.getAlbumName()).thenReturn("Album A");
		when(track.getAlbumImageUrl()).thenReturn("https://image.example.com/album-a.jpg");
		when(track.getDurationMs()).thenReturn(180000);
		when(track.getSpotifyTrackId()).thenReturn("spotify-track-4001");
		when(track.getSpotifyTrackUrl())
			.thenReturn("https://open.spotify.com/track/spotify-track-4001");
		when(track.getPreviewUrl()).thenReturn("https://preview.example.com/4001.mp3");
		when(firstArtist.getArtistId()).thenReturn(11L);
		when(firstArtist.getArtistName()).thenReturn("Artist A");
		when(secondArtist.getArtistId()).thenReturn(12L);
		when(secondArtist.getArtistName()).thenReturn("Artist B");

		when(prestudyPlaylistRepository.findByPrestudyPlaylistIdAndUserId(701L, 1L))
			.thenReturn(Optional.of(playlist));
		when(prestudyPlaylistTrackRepository.findByPrestudyPlaylistIdOrderByTrackOrderAsc(701L))
			.thenReturn(List.of(playlistTrack));
		when(trackRepository.findAllById(List.of(4001L))).thenReturn(List.of(track));
		when(trackArtistRepository.findByTrackIdInOrderByTrackIdAscArtistOrderAsc(List.of(4001L)))
			.thenReturn(List.of(
				new TrackArtist(4001L, 11L, (short)1),
				new TrackArtist(4001L, 12L, (short)2)));
		when(artistRepository.findAllById(List.of(11L, 12L)))
			.thenReturn(List.of(firstArtist, secondArtist));

		List<PrestudyPlaylistTrackResponse> result = service.getPrestudyPlaylistTracks(1L, 701L);

		assertThat(result).singleElement().satisfies(response -> {
			assertThat(response.trackId()).isEqualTo(4001L);
			assertThat(response.trackName()).isEqualTo("Song A");
			assertThat(response.artistName()).isEqualTo("Artist A, Artist B");
			assertThat(response.albumName()).isEqualTo("Album A");
			assertThat(response.albumImageUrl())
				.isEqualTo("https://image.example.com/album-a.jpg");
			assertThat(response.durationMs()).isEqualTo(180000);
			assertThat(response.spotifyTrackId()).isEqualTo("spotify-track-4001");
			assertThat(response.spotifyTrackUrl())
				.isEqualTo("https://open.spotify.com/track/spotify-track-4001");
			assertThat(response.previewUrl()).isEqualTo("https://preview.example.com/4001.mp3");
		});
	}

	@Test
	void removesSpotifyPlaylistBeforeDeletingSetpikRecords() {
		PrestudyPlaylist playlist = mock(PrestudyPlaylist.class);
		SpotifyAccount account = mock(SpotifyAccount.class);
		when(playlist.getSpotifyPlaylistId()).thenReturn("spotify-playlist");
		when(prestudyPlaylistRepository.findByPrestudyPlaylistIdAndUserId(701L, 1L))
			.thenReturn(Optional.of(playlist));
		when(spotifyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(account));
		when(account.getConnectionStatus()).thenReturn(ConnectionStatus.CONNECTED);
		when(account.getTokenExpiresAt()).thenReturn(LocalDateTime.of(2026, 8, 14, 20, 0));
		when(account.getAccessTokenEncrypted()).thenReturn("encrypted-token");
		when(tokenCipher.decrypt("encrypted-token")).thenReturn("access-token");

		service.deletePrestudyPlaylist(1L, 701L);

		verify(spotifyPlaylistClient)
			.removePlaylistFromLibrary("access-token", "spotify-playlist");
		verify(prestudyPlaylistTrackRepository).deleteByPrestudyPlaylistId(701L);
		verify(prestudyPlaylistRepository).delete(playlist);
	}

	@Test
	void keepsSetpikRecordsWhenSpotifyRemovalFails() {
		PrestudyPlaylist playlist = mock(PrestudyPlaylist.class);
		SpotifyAccount account = mock(SpotifyAccount.class);
		when(playlist.getSpotifyPlaylistId()).thenReturn("spotify-playlist");
		when(prestudyPlaylistRepository.findByPrestudyPlaylistIdAndUserId(701L, 1L))
			.thenReturn(Optional.of(playlist));
		when(spotifyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(account));
		when(account.getConnectionStatus()).thenReturn(ConnectionStatus.CONNECTED);
		when(account.getTokenExpiresAt()).thenReturn(LocalDateTime.of(2026, 8, 14, 20, 0));
		when(account.getAccessTokenEncrypted()).thenReturn("encrypted-token");
		when(tokenCipher.decrypt("encrypted-token")).thenReturn("access-token");
		org.mockito.Mockito.doThrow(new SpotifyPlaylistApiException("failed"))
			.when(spotifyPlaylistClient)
			.removePlaylistFromLibrary("access-token", "spotify-playlist");

		org.assertj.core.api.Assertions.assertThatThrownBy(
			() -> service.deletePrestudyPlaylist(1L, 701L))
			.isInstanceOf(BusinessException.class);

		verify(prestudyPlaylistTrackRepository, never()).deleteByPrestudyPlaylistId(701L);
		verify(prestudyPlaylistRepository, never()).delete(playlist);
	}

	@Test
	void returnsOnlyTracksFromTheAnalyzedOriginalPlaylist() {
		PlaylistAnalysis analysis = completedAnalysis(1L, 10L);
		Performance performance = mock(Performance.class);
		Artist artist = mock(Artist.class);
		Track originalTrack = mock(Track.class);
		Track secondOriginalTrack = mock(Track.class);
		when(artist.getArtistId()).thenReturn(7L);
		when(artist.getArtistName()).thenReturn("Artist A");
		when(originalTrack.getTrackId()).thenReturn(4001L);
		when(originalTrack.getTrackName()).thenReturn("Song A");
		when(secondOriginalTrack.getTrackId()).thenReturn(4002L);
		when(secondOriginalTrack.getTrackName()).thenReturn("Song B");

		when(playlistAnalysisRepository.findByAnalysisIdAndUserId(501L, 1L))
			.thenReturn(Optional.of(analysis));
		when(performanceRepository.findByPerformanceIdAndIsDeletedFalse(1001L))
			.thenReturn(Optional.of(performance));
		when(performanceMatchRepository.findByAnalysisIdAndPerformanceId(501L, 1001L))
			.thenReturn(Optional.of(mock(PerformanceMatch.class)));
		when(performanceArtistRepository.findByPerformanceIdOrderByLineupOrderAsc(1001L))
			.thenReturn(List.of(new PerformanceArtist(7L, 1001L, 1L, true)));
		when(artistRepository.findAllById(List.of(7L))).thenReturn(List.of(artist));
		when(playlistTrackRepository.findByPlaylistIdOrderByTrackPositionAsc(10L))
			.thenReturn(List.of(
				new PlaylistTrack(10L, 4001L, 1, LocalDateTime.now()),
				new PlaylistTrack(10L, 4002L, 2, LocalDateTime.now())));
		when(trackRepository.findAllById(List.of(4001L, 4002L)))
			.thenReturn(List.of(originalTrack, secondOriginalTrack));
		when(trackArtistRepository.findByTrackIdInOrderByTrackIdAscArtistOrderAsc(List.of(4001L, 4002L)))
			.thenReturn(List.of(
				new TrackArtist(4001L, 7L, (short) 1),
				new TrackArtist(4002L, 7L, (short) 1)));

		var result = service.getCandidates(1L, 1001L, 501L);

		assertThat(result.artists()).hasSize(1);
		assertThat(result.artists().get(0).isFromOriginalPlaylist()).isTrue();
		assertThat(result.artists().get(0).candidateTracks())
			.extracting(candidate -> candidate.trackId())
			.containsExactly(4001L, 4002L);
		assertThat(result.artists().get(0).candidateTracks().get(0).sourceType())
			.isEqualTo("ORIGINAL_PLAYLIST");
	}

	@Test
	void resolvesKopisLineupAliasToOriginalSpotifyArtistTrack() {
		PlaylistAnalysis analysis = completedAnalysis(1L, 10L);
		Performance performance = mock(Performance.class);
		PerformanceMatch match = mock(PerformanceMatch.class);
		when(match.getMatchId()).thenReturn(900L);
		Artist kopisArtist = mock(Artist.class);
		when(kopisArtist.getArtistId()).thenReturn(838L);
		when(kopisArtist.getArtistName()).thenReturn("최예나");
		Artist spotifyArtist = mock(Artist.class);
		when(spotifyArtist.getArtistId()).thenReturn(76L);
		when(spotifyArtist.getSpotifyArtistId()).thenReturn("spotify-yena");
		Track originalTrack = mock(Track.class);
		Track spotifyTrack = mock(Track.class);
		when(originalTrack.getTrackId()).thenReturn(4001L);
		when(originalTrack.getTrackName()).thenReturn("YENA Song");
		when(spotifyTrack.getTrackId()).thenReturn(4002L);
		when(spotifyTrack.getTrackName()).thenReturn("YENA New Song");

		when(playlistAnalysisRepository.findByAnalysisIdAndUserId(501L, 1L))
			.thenReturn(Optional.of(analysis));
		when(performanceRepository.findByPerformanceIdAndIsDeletedFalse(1001L))
			.thenReturn(Optional.of(performance));
		when(performanceMatchRepository.findByAnalysisIdAndPerformanceId(501L, 1001L))
			.thenReturn(Optional.of(match));
		when(performanceArtistRepository.findByPerformanceIdOrderByLineupOrderAsc(1001L))
			.thenReturn(List.of(new PerformanceArtist(838L, 1001L, 1L, true)));
		when(artistRepository.findAllById(List.of(838L))).thenReturn(List.of(kopisArtist));
		when(artistAliasRepository.findByKopisArtistIdIn(List.of(838L))).thenReturn(List.of(
			ArtistAlias.resolved(838L, "spotify-yena", "WIKIDATA", "Q1", LocalDateTime.now())
		));
		when(artistRepository.findBySpotifyArtistIdIn(List.of("spotify-yena")))
			.thenReturn(List.of(spotifyArtist));
		when(performanceMatchArtistRepository.findByMatchId(900L)).thenReturn(List.of());
		when(playlistTrackRepository.findByPlaylistIdOrderByTrackPositionAsc(10L))
			.thenReturn(List.of(new PlaylistTrack(10L, 4001L, 1, LocalDateTime.now())));
		when(trackRepository.findAllById(List.of(4001L))).thenReturn(List.of(originalTrack));
		when(trackArtistRepository.findByTrackIdInOrderByTrackIdAscArtistOrderAsc(List.of(4001L)))
			.thenReturn(List.of(new TrackArtist(4001L, 76L, (short) 1)));
		stubConnectedSpotifyAccount();
		when(spotifyPlaylistClient.fetchRepresentativeTracks(
			"access-token", "spotify-yena", 20))
			.thenReturn(List.of(new SpotifyTrackSnapshot(
				"spotify-new", "YENA New Song", "Album", null, null, null,
				180000, true, null, List.of())));
		when(trackRepository.findBySpotifyTrackId("spotify-new"))
			.thenReturn(Optional.of(spotifyTrack));

		var result = service.getCandidates(1L, 1001L, 501L);

		assertThat(result.artists()).singleElement().satisfies(candidate -> {
			assertThat(candidate.artistId()).isEqualTo(838L);
			assertThat(candidate.artistName()).isEqualTo("최예나");
			assertThat(candidate.isFromOriginalPlaylist()).isTrue();
			assertThat(candidate.candidateTracks())
				.extracting(track -> track.trackId(), track -> track.sourceType())
				.containsExactly(
					org.assertj.core.groups.Tuple.tuple(4001L, "ORIGINAL_PLAYLIST"),
					org.assertj.core.groups.Tuple.tuple(4002L, "MATCHED_ARTIST"));
		});
	}

	@Test
	void usesMatchedSpotifyArtistWhenKopisLineupIsEmpty() {
		PlaylistAnalysis analysis = completedAnalysis(1L, 10L);
		Performance performance = mock(Performance.class);
		PerformanceMatch match = mock(PerformanceMatch.class);
		when(match.getMatchId()).thenReturn(900L);
		Artist fromis = mock(Artist.class);
		when(fromis.getArtistId()).thenReturn(75L);
		when(fromis.getArtistName()).thenReturn("fromis_9");
		when(fromis.getSpotifyArtistId()).thenReturn("spotify-fromis");
		Track originalTrack = mock(Track.class);
		when(originalTrack.getTrackId()).thenReturn(4001L);
		when(originalTrack.getTrackName()).thenReturn("fromis Song");

		when(playlistAnalysisRepository.findByAnalysisIdAndUserId(501L, 1L))
			.thenReturn(Optional.of(analysis));
		when(performanceRepository.findByPerformanceIdAndIsDeletedFalse(1001L))
			.thenReturn(Optional.of(performance));
		when(performanceMatchRepository.findByAnalysisIdAndPerformanceId(501L, 1001L))
			.thenReturn(Optional.of(match));
		when(performanceArtistRepository.findByPerformanceIdOrderByLineupOrderAsc(1001L))
			.thenReturn(List.of());
		when(performanceMatchArtistRepository.findByMatchId(900L))
			.thenReturn(List.of(PerformanceMatchArtist.create(900L, 75L, 6)));
		when(artistRepository.findAllById(List.of(75L))).thenReturn(List.of(fromis));
		when(playlistTrackRepository.findByPlaylistIdOrderByTrackPositionAsc(10L))
			.thenReturn(List.of(new PlaylistTrack(10L, 4001L, 1, LocalDateTime.now())));
		when(trackRepository.findAllById(List.of(4001L))).thenReturn(List.of(originalTrack));
		when(trackArtistRepository.findByTrackIdInOrderByTrackIdAscArtistOrderAsc(List.of(4001L)))
			.thenReturn(List.of(new TrackArtist(4001L, 75L, (short) 1)));
		stubConnectedSpotifyAccount();

		var result = service.getCandidates(1L, 1001L, 501L);

		assertThat(result.artists()).singleElement().satisfies(candidate -> {
			assertThat(candidate.artistId()).isEqualTo(75L);
			assertThat(candidate.artistName()).isEqualTo("fromis_9");
			assertThat(candidate.candidateTracks()).singleElement()
				.satisfies(track -> assertThat(track.trackId()).isEqualTo(4001L));
		});
	}

	@Test
	void storesActualSourceTypeAndTrackOrderWhenCreatingPlaylist() {
		PlaylistAnalysis analysis = completedAnalysis(1L, 10L);
		Performance performance = mock(Performance.class);
		Artist artistA = mock(Artist.class);
		Artist artistB = mock(Artist.class);
		when(artistA.getArtistId()).thenReturn(7L);
		when(artistB.getArtistId()).thenReturn(9L);
		Track originalTrack = mock(Track.class);
		Track matchedTrack = mock(Track.class);
		when(originalTrack.getTrackId()).thenReturn(4001L);
		when(originalTrack.getSpotifyTrackId()).thenReturn("spotify-original");
		when(matchedTrack.getTrackId()).thenReturn(4002L);
		when(matchedTrack.getSpotifyTrackId()).thenReturn("spotify-matched");

		when(playlistAnalysisRepository.findByAnalysisIdAndUserId(501L, 1L))
			.thenReturn(Optional.of(analysis));
		when(performanceRepository.findByPerformanceIdAndIsDeletedFalse(1001L))
			.thenReturn(Optional.of(performance));
		when(performanceMatchRepository.findByAnalysisIdAndPerformanceId(501L, 1001L))
			.thenReturn(Optional.of(mock(PerformanceMatch.class)));
		when(performanceArtistRepository.findByPerformanceIdOrderByLineupOrderAsc(1001L))
			.thenReturn(List.of(
				new PerformanceArtist(7L, 1001L, 1L, true),
				new PerformanceArtist(9L, 1001L, 2L, false)));
		when(artistRepository.findAllById(List.of(7L, 9L))).thenReturn(List.of(artistA, artistB));
		when(playlistTrackRepository.findByPlaylistIdOrderByTrackPositionAsc(10L))
			.thenReturn(List.of(new PlaylistTrack(10L, 4001L, 1, LocalDateTime.now())));
		when(trackRepository.findAllById(List.of(4001L, 4002L)))
			.thenReturn(List.of(originalTrack, matchedTrack));
		when(trackRepository.findAllById(List.of(4001L))).thenReturn(List.of(originalTrack));
		when(trackArtistRepository.findByTrackIdInOrderByTrackIdAscArtistOrderAsc(List.of(4001L)))
			.thenReturn(List.of(new TrackArtist(4001L, 7L, (short) 1)));
		when(trackArtistRepository.findByTrackIdInOrderByTrackIdAscArtistOrderAsc(
			List.of(4001L, 4002L)))
			.thenReturn(List.of(
				new TrackArtist(4001L, 7L, (short) 1),
				new TrackArtist(4002L, 9L, (short) 1)));

		SpotifyAccount account = mock(SpotifyAccount.class);
		when(account.getConnectionStatus()).thenReturn(ConnectionStatus.CONNECTED);
		when(account.getTokenExpiresAt()).thenReturn(LocalDateTime.of(2026, 8, 14, 20, 0));
		when(account.getAccessTokenEncrypted()).thenReturn("encrypted-token");
		when(spotifyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(account));
		when(tokenCipher.decrypt("encrypted-token")).thenReturn("access-token");
		when(spotifyPlaylistClient.createPlaylist("access-token", "Prestudy", false))
			.thenReturn("spotify-playlist");

		PrestudyPlaylist savedPlaylist = mock(PrestudyPlaylist.class);
		when(savedPlaylist.getPrestudyPlaylistId()).thenReturn(701L);
		when(prestudyPlaylistRepository.save(org.mockito.ArgumentMatchers.any(PrestudyPlaylist.class)))
			.thenReturn(savedPlaylist);

		var result = service.createPrestudyPlaylist(
			1L, 1001L,
			new CreatePrestudyPlaylistRequest(
				"Prestudy", false, 501L, List.of(4001L, 4002L)));

		assertThat(result.prestudyPlaylistId()).isEqualTo(701L);
		assertThat(result.trackCount()).isEqualTo(2);
		verify(spotifyPlaylistClient).addTracks(
			"access-token", "spotify-playlist", List.of("spotify-original", "spotify-matched"));
		ArgumentCaptor<PrestudyPlaylistTrack> trackCaptor =
			ArgumentCaptor.forClass(PrestudyPlaylistTrack.class);
		verify(prestudyPlaylistTrackRepository, org.mockito.Mockito.times(2))
			.save(trackCaptor.capture());
		assertThat(trackCaptor.getAllValues())
			.extracting(PrestudyPlaylistTrack::getSourceType)
			.containsExactly(SourceType.ORIGINAL_PLAYLIST, SourceType.MATCHED_ARTIST);
		assertThat(trackCaptor.getAllValues())
			.extracting(PrestudyPlaylistTrack::getIsNewArtistTrack)
			.containsExactly(false, true);
	}

	private PlaylistAnalysis completedAnalysis(Long userId, Long playlistId) {
		return new PlaylistAnalysis(
			userId, playlistId, "spotify-playlist", "Playlist", null,
			2, 2, AnalysisStatus.COMPLETED, null);
	}

	private void stubConnectedSpotifyAccount() {
		SpotifyAccount account = mock(SpotifyAccount.class);
		when(account.getConnectionStatus()).thenReturn(ConnectionStatus.CONNECTED);
		when(account.getTokenExpiresAt()).thenReturn(LocalDateTime.of(2026, 8, 14, 20, 0));
		when(account.getAccessTokenEncrypted()).thenReturn("encrypted-token");
		when(spotifyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(account));
		when(tokenCipher.decrypt("encrypted-token")).thenReturn("access-token");
	}
}

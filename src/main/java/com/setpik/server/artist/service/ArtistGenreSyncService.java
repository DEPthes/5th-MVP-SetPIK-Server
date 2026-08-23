package com.setpik.server.artist.service;

import com.setpik.server.artist.client.WikidataArtistGenreClient;
import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.domain.ArtistAlias;
import com.setpik.server.artist.domain.ArtistAliasResolutionStatus;
import com.setpik.server.artist.domain.ArtistGenre;
import com.setpik.server.artist.domain.ArtistGenreId;
import com.setpik.server.artist.domain.ArtistGenreSyncStatus;
import com.setpik.server.artist.domain.Genre;
import com.setpik.server.artist.dto.ArtistGenreSyncResponse;
import com.setpik.server.artist.repository.ArtistAliasRepository;
import com.setpik.server.artist.repository.ArtistGenreRepository;
import com.setpik.server.artist.repository.ArtistGenreSyncStatusRepository;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.artist.repository.GenreRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ArtistGenreSyncService {
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final int MAX_BATCH_SIZE = 100;
	private final ArtistRepository artistRepository;
	private final ArtistAliasRepository aliasRepository;
	private final GenreRepository genreRepository;
	private final ArtistGenreRepository artistGenreRepository;
	private final ArtistGenreSyncStatusRepository statusRepository;
	private final WikidataArtistGenreClient wikidataClient;

	public ArtistGenreSyncService(ArtistRepository artistRepository, ArtistAliasRepository aliasRepository,
		GenreRepository genreRepository, ArtistGenreRepository artistGenreRepository,
		ArtistGenreSyncStatusRepository statusRepository, WikidataArtistGenreClient wikidataClient) {
		this.artistRepository = artistRepository;
		this.aliasRepository = aliasRepository;
		this.genreRepository = genreRepository;
		this.artistGenreRepository = artistGenreRepository;
		this.statusRepository = statusRepository;
		this.wikidataClient = wikidataClient;
	}

	public ArtistGenreSyncResponse syncPendingGenres(int requestedLimit) {
		int limit = Math.max(1, Math.min(requestedLimit, MAX_BATCH_SIZE));
		List<Long> candidateIds = artistRepository.findPendingGenreSyncArtistIds(limit);
		Map<Long, Artist> artists = artistRepository.findAllById(candidateIds).stream()
			.collect(Collectors.toMap(Artist::getArtistId, Function.identity()));
		Map<Long, String> aliasSpotifyIds = aliasRepository.findByKopisArtistIdIn(candidateIds).stream()
			.filter(alias -> alias.getResolutionStatus() == ArtistAliasResolutionStatus.RESOLVED)
			.collect(Collectors.toMap(ArtistAlias::getKopisArtistId, ArtistAlias::getSpotifyArtistId));
		LocalDateTime attemptedAt = LocalDateTime.now(KST);
		int resolved = 0, savedGenres = 0, notFound = 0, failed = 0;

		for (Long artistId : candidateIds) {
			Artist artist = artists.get(artistId);
			if (artist == null) continue;
			String spotifyId = artist.getSpotifyArtistId() != null
				? artist.getSpotifyArtistId() : aliasSpotifyIds.get(artistId);
			WikidataArtistGenreClient.LookupResult lookup = wikidataClient.lookup(spotifyId);
			if (lookup.status() == WikidataArtistGenreClient.Status.FAILED) {
				failed++;
				statusRepository.save(ArtistGenreSyncStatus.of(artistId, ArtistAliasResolutionStatus.FAILED, null, attemptedAt));
				continue;
			}
			Set<String> canonicalGenres = ExternalGenreMapper.toCanonicalGenres(lookup.genreLabels());
			if (lookup.status() == WikidataArtistGenreClient.Status.NOT_FOUND || canonicalGenres.isEmpty()) {
				notFound++;
				statusRepository.save(ArtistGenreSyncStatus.of(artistId, ArtistAliasResolutionStatus.NOT_FOUND,
					lookup.externalEntityId(), attemptedAt));
				continue;
			}
			for (String genreName : canonicalGenres) {
				String normalized = Artist.normalize(genreName);
				Genre genre = genreRepository.findByNormalizedName(normalized)
					.orElseGet(() -> genreRepository.save(new Genre(genreName, normalized)));
				ArtistGenreId id = new ArtistGenreId(artistId, genre.getGenreId());
				if (!artistGenreRepository.existsById(id)) {
					artistGenreRepository.save(new ArtistGenre(artistId, genre.getGenreId(), "WIKIDATA"));
					savedGenres++;
				}
			}
			resolved++;
			statusRepository.save(ArtistGenreSyncStatus.of(artistId, ArtistAliasResolutionStatus.RESOLVED,
				lookup.externalEntityId(), attemptedAt));
		}
		return new ArtistGenreSyncResponse(candidateIds.size(), resolved, savedGenres, notFound, failed,
			OffsetDateTime.now(KST));
	}
}

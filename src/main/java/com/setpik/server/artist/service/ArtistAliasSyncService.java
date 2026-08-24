package com.setpik.server.artist.service;

import com.setpik.server.artist.client.WikidataArtistAliasClient;
import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.domain.ArtistAlias;
import com.setpik.server.artist.domain.ArtistAliasResolutionStatus;
import com.setpik.server.artist.domain.SpotifyArtistAliasSyncStatus;
import com.setpik.server.artist.dto.ArtistAliasSyncResponse;
import com.setpik.server.artist.repository.ArtistAliasRepository;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.artist.repository.SpotifyArtistAliasSyncStatusRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ArtistAliasSyncService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final String MUSIC_GENRE_KEYWORD = "음악";
	private static final int MAX_BATCH_SIZE = 100;

	private final ArtistRepository artistRepository;
	private final ArtistAliasRepository artistAliasRepository;
	private final WikidataArtistAliasClient wikidataClient;
	private final SpotifyArtistAliasSyncStatusRepository reverseSyncStatusRepository;

	public ArtistAliasSyncService(
		ArtistRepository artistRepository,
		ArtistAliasRepository artistAliasRepository,
		WikidataArtistAliasClient wikidataClient,
		SpotifyArtistAliasSyncStatusRepository reverseSyncStatusRepository
	) {
		this.artistRepository = artistRepository;
		this.artistAliasRepository = artistAliasRepository;
		this.wikidataClient = wikidataClient;
		this.reverseSyncStatusRepository = reverseSyncStatusRepository;
	}

	public ArtistAliasSyncResponse syncPendingAliases(int requestedLimit) {
		int limit = Math.max(1, Math.min(requestedLimit, MAX_BATCH_SIZE));
		LocalDateTime attemptedAt = LocalDateTime.now(KST);
		SyncCounts counts = new SyncCounts();

		List<Long> reverseCandidateIds = artistRepository.findPendingReverseAliasSyncArtistIds(limit);
		syncReverseAliases(reverseCandidateIds, attemptedAt, counts);

		int remainingLimit = limit - reverseCandidateIds.size();
		List<Long> candidateIds = remainingLimit <= 0 ? List.of() : artistRepository
			.findUnresolvedKopisMusicArtistIds(MUSIC_GENRE_KEYWORD, remainingLimit);
		syncForwardAliases(candidateIds, attemptedAt, counts);

		return new ArtistAliasSyncResponse(reverseCandidateIds.size() + candidateIds.size(),
			counts.resolved, counts.notFound, counts.ambiguous, counts.failed, OffsetDateTime.now(KST));
	}

	private void syncReverseAliases(List<Long> candidateIds, LocalDateTime attemptedAt, SyncCounts counts) {
		Map<Long, Artist> artistsById = artistRepository.findAllById(candidateIds).stream()
			.collect(Collectors.toMap(Artist::getArtistId, Function.identity()));
		List<String> spotifyIds = candidateIds.stream()
			.map(artistsById::get)
			.filter(java.util.Objects::nonNull)
			.map(Artist::getSpotifyArtistId)
			.filter(java.util.Objects::nonNull)
			.distinct()
			.toList();
		Map<String, WikidataArtistAliasClient.ReverseLookupResult> results =
			wikidataClient.resolveBySpotifyIds(spotifyIds);

		for (Long candidateId : candidateIds) {
			Artist spotifyArtist = artistsById.get(candidateId);
			if (spotifyArtist == null || spotifyArtist.getSpotifyArtistId() == null) continue;
			String spotifyId = spotifyArtist.getSpotifyArtistId();
			WikidataArtistAliasClient.ReverseLookupResult result = results.getOrDefault(
				spotifyId, WikidataArtistAliasClient.ReverseLookupResult.failed());
			ArtistAliasResolutionStatus status = resolveReverseAlias(spotifyId, result, attemptedAt);
			increment(counts, status);
			reverseSyncStatusRepository.save(SpotifyArtistAliasSyncStatus.of(
				candidateId, status, result.externalEntityId(), attemptedAt));
		}
	}

	private ArtistAliasResolutionStatus resolveReverseAlias(String spotifyId,
		WikidataArtistAliasClient.ReverseLookupResult result, LocalDateTime attemptedAt) {
		if (result.status() != WikidataArtistAliasClient.Status.RESOLVED) {
			return toResolutionStatus(result.status());
		}
		if (result.koreanNames().isEmpty()) {
			return ArtistAliasResolutionStatus.NOT_FOUND;
		}

		List<String> normalizedNames = result.koreanNames().stream()
			.map(Artist::normalize)
			.distinct()
			.toList();
		List<Artist> matchingKopisArtists = artistRepository.findByNormalizedNameIn(normalizedNames).stream()
			.filter(artist -> !Boolean.TRUE.equals(artist.getSpotifyAvailable()))
			.toList();
		Set<Long> matchingIds = matchingKopisArtists.stream()
			.map(Artist::getArtistId)
			.collect(Collectors.toCollection(LinkedHashSet::new));

		if (matchingIds.size() == 1) {
			Long kopisArtistId = matchingIds.iterator().next();
			ArtistAlias existing = artistAliasRepository.findById(kopisArtistId).orElse(null);
			if (existing != null && existing.getResolutionStatus() == ArtistAliasResolutionStatus.RESOLVED
				&& !spotifyId.equals(existing.getSpotifyArtistId())) {
				return ArtistAliasResolutionStatus.AMBIGUOUS;
			}
			artistAliasRepository.save(ArtistAlias.resolved(kopisArtistId, spotifyId, "WIKIDATA",
				result.externalEntityId(), attemptedAt));
			return ArtistAliasResolutionStatus.RESOLVED;
		}

		if (matchingIds.isEmpty()) {
			invalidateContradictedAliases(spotifyId, attemptedAt);
			return ArtistAliasResolutionStatus.NOT_FOUND;
		}
		return ArtistAliasResolutionStatus.AMBIGUOUS;
	}

	private void invalidateContradictedAliases(String spotifyId, LocalDateTime attemptedAt) {
		for (ArtistAlias existing : artistAliasRepository.findBySpotifyArtistId(spotifyId)) {
			artistAliasRepository.save(ArtistAlias.unresolved(existing.getKopisArtistId(),
				ArtistAliasResolutionStatus.AMBIGUOUS, attemptedAt));
		}
	}

	private void syncForwardAliases(List<Long> candidateIds, LocalDateTime attemptedAt, SyncCounts counts) {
		Map<Long, Artist> artistsById = artistRepository.findAllById(candidateIds).stream()
			.collect(Collectors.toMap(Artist::getArtistId, Function.identity()));

		for (Long candidateId : candidateIds) {
			Artist artist = artistsById.get(candidateId);
			if (artist == null) continue;
			WikidataArtistAliasClient.LookupResult result = wikidataClient.resolve(artist.getArtistName());
			ArtistAlias alias = switch (result.status()) {
				case RESOLVED -> {
					counts.resolved++;
					yield ArtistAlias.resolved(candidateId, result.spotifyArtistId(), "WIKIDATA",
						result.externalEntityId(), attemptedAt);
				}
				case NOT_FOUND -> {
					counts.notFound++;
					yield ArtistAlias.unresolved(candidateId, ArtistAliasResolutionStatus.NOT_FOUND, attemptedAt);
				}
				case AMBIGUOUS -> {
					counts.ambiguous++;
					yield ArtistAlias.unresolved(candidateId, ArtistAliasResolutionStatus.AMBIGUOUS, attemptedAt);
				}
				case FAILED -> {
					counts.failed++;
					yield ArtistAlias.unresolved(candidateId, ArtistAliasResolutionStatus.FAILED, attemptedAt);
				}
			};
			artistAliasRepository.save(alias);
		}
	}

	private ArtistAliasResolutionStatus toResolutionStatus(WikidataArtistAliasClient.Status status) {
		return switch (status) {
			case RESOLVED -> ArtistAliasResolutionStatus.RESOLVED;
			case NOT_FOUND -> ArtistAliasResolutionStatus.NOT_FOUND;
			case AMBIGUOUS -> ArtistAliasResolutionStatus.AMBIGUOUS;
			case FAILED -> ArtistAliasResolutionStatus.FAILED;
		};
	}

	private void increment(SyncCounts counts, ArtistAliasResolutionStatus status) {
		switch (status) {
			case RESOLVED -> counts.resolved++;
			case NOT_FOUND -> counts.notFound++;
			case AMBIGUOUS -> counts.ambiguous++;
			case FAILED -> counts.failed++;
		}
	}

	private static class SyncCounts {
		private int resolved;
		private int notFound;
		private int ambiguous;
		private int failed;
	}
}

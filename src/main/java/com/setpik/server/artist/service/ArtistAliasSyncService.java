package com.setpik.server.artist.service;

import com.setpik.server.artist.client.WikidataArtistAliasClient;
import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.domain.ArtistAlias;
import com.setpik.server.artist.domain.ArtistAliasResolutionStatus;
import com.setpik.server.artist.dto.ArtistAliasSyncResponse;
import com.setpik.server.artist.repository.ArtistAliasRepository;
import com.setpik.server.artist.repository.ArtistRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
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

	public ArtistAliasSyncService(
		ArtistRepository artistRepository,
		ArtistAliasRepository artistAliasRepository,
		WikidataArtistAliasClient wikidataClient
	) {
		this.artistRepository = artistRepository;
		this.artistAliasRepository = artistAliasRepository;
		this.wikidataClient = wikidataClient;
	}

	public ArtistAliasSyncResponse syncPendingAliases(int requestedLimit) {
		int limit = Math.max(1, Math.min(requestedLimit, MAX_BATCH_SIZE));
		List<Long> candidateIds = artistRepository
			.findUnresolvedKopisMusicArtistIds(MUSIC_GENRE_KEYWORD, limit);
		Map<Long, Artist> artistsById = artistRepository.findAllById(candidateIds).stream()
			.collect(Collectors.toMap(Artist::getArtistId, Function.identity()));
		LocalDateTime attemptedAt = LocalDateTime.now(KST);
		int resolved = 0;
		int notFound = 0;
		int ambiguous = 0;
		int failed = 0;

		for (Long candidateId : candidateIds) {
			Artist artist = artistsById.get(candidateId);
			if (artist == null) continue;
			WikidataArtistAliasClient.LookupResult result = wikidataClient.resolve(artist.getArtistName());
			ArtistAlias alias = switch (result.status()) {
				case RESOLVED -> {
					resolved++;
					yield ArtistAlias.resolved(candidateId, result.spotifyArtistId(), "WIKIDATA",
						result.externalEntityId(), attemptedAt);
				}
				case NOT_FOUND -> {
					notFound++;
					yield ArtistAlias.unresolved(candidateId, ArtistAliasResolutionStatus.NOT_FOUND, attemptedAt);
				}
				case AMBIGUOUS -> {
					ambiguous++;
					yield ArtistAlias.unresolved(candidateId, ArtistAliasResolutionStatus.AMBIGUOUS, attemptedAt);
				}
				case FAILED -> {
					failed++;
					yield ArtistAlias.unresolved(candidateId, ArtistAliasResolutionStatus.FAILED, attemptedAt);
				}
			};
			artistAliasRepository.save(alias);
		}

		return new ArtistAliasSyncResponse(candidateIds.size(), resolved, notFound, ambiguous, failed,
			OffsetDateTime.now(KST));
	}
}

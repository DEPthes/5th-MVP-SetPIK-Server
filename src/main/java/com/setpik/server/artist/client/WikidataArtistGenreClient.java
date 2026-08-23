package com.setpik.server.artist.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;

/** Spotify artist ID(P1902)로 Wikidata 장르(P136)를 조회한다. */
@Component
public class WikidataArtistGenreClient {
	private final RestClient restClient;

	public WikidataArtistGenreClient(RestClient.Builder builder) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(3000);
		factory.setReadTimeout(10000);
		this.restClient = builder.baseUrl("https://query.wikidata.org")
			.defaultHeader("User-Agent", "SetPIK-ArtistGenreResolver/1.0")
			.requestFactory(factory).build();
	}

	public LookupResult lookup(String spotifyArtistId) {
		try {
			URI uri = URI.create("https://query.wikidata.org/sparql?query="
				+ UriUtils.encodeQueryParam(query(spotifyArtistId), StandardCharsets.UTF_8)
				+ "&format=json");
			WikidataResponse response = restClient.get().uri(uri).accept(MediaType.APPLICATION_JSON)
				.retrieve().body(WikidataResponse.class);
			List<Binding> bindings = response == null || response.results() == null
				? List.of() : response.results().bindings();
			if (bindings == null || bindings.isEmpty()) return LookupResult.notFound();
			Set<String> genreLabels = new LinkedHashSet<>();
			String entityId = null;
			for (Binding binding : bindings) {
				if (binding.artist() != null && binding.artist().value() != null) entityId = entityId(binding.artist().value());
				if (binding.genreLabel() != null && binding.genreLabel().value() != null) genreLabels.add(binding.genreLabel().value());
			}
			return entityId == null || genreLabels.isEmpty()
				? LookupResult.notFound() : LookupResult.resolved(entityId, genreLabels);
		} catch (RestClientException | IllegalArgumentException exception) {
			return LookupResult.failed();
		}
	}

	private String query(String spotifyArtistId) {
		return """
			PREFIX wdt: <http://www.wikidata.org/prop/direct/>
			PREFIX wikibase: <http://wikiba.se/ontology#>
			PREFIX bd: <http://www.bigdata.com/rdf#>
			SELECT DISTINCT ?artist ?genre ?genreLabel WHERE {
			  ?artist wdt:P1902 "%s" .
			  ?artist wdt:P136 ?genre .
			  SERVICE wikibase:label { bd:serviceParam wikibase:language "en,ko". }
			}
			""".formatted(escape(spotifyArtistId));
	}

	private String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
	private String entityId(String url) { int i = url.lastIndexOf('/'); return i < 0 ? url : url.substring(i + 1); }

	public record LookupResult(Status status, String externalEntityId, Set<String> genreLabels) {
		public static LookupResult resolved(String id, Set<String> labels) { return new LookupResult(Status.RESOLVED, id, Set.copyOf(labels)); }
		public static LookupResult notFound() { return new LookupResult(Status.NOT_FOUND, null, Set.of()); }
		public static LookupResult failed() { return new LookupResult(Status.FAILED, null, Set.of()); }
	}
	public enum Status { RESOLVED, NOT_FOUND, FAILED }
	private record WikidataResponse(Results results) { }
	private record Results(List<Binding> bindings) { }
	private record Binding(Value artist, Value genre, Value genreLabel) { }
	private record Value(String value) { }
}

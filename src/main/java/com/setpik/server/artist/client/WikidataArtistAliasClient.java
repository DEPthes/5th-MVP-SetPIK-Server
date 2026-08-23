package com.setpik.server.artist.client;

import java.net.URI;
import java.util.List;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Wikidata의 정확한 한국어 label/alias와 Spotify artist ID(P1902)가 함께 존재할 때만 Alias 후보를 확정한다.
 */
@Component
public class WikidataArtistAliasClient {

	private static final String SOURCE_TYPE = "WIKIDATA";
	private final RestClient restClient;

	public WikidataArtistAliasClient(RestClient.Builder restClientBuilder) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(3000);
		requestFactory.setReadTimeout(10000);
		this.restClient = restClientBuilder
			.baseUrl("https://query.wikidata.org")
			.defaultHeader("User-Agent", "SetPIK-ArtistAliasResolver/1.0")
			.requestFactory(requestFactory)
			.build();
	}

	public LookupResult resolve(String kopisArtistName) {
		try {
			URI requestUri = URI.create("https://query.wikidata.org/sparql?query="
				+ UriUtils.encodeQueryParam(exactAliasQuery(kopisArtistName), StandardCharsets.UTF_8)
				+ "&format=json");
			WikidataResponse response = restClient.get()
				.uri(requestUri)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.body(WikidataResponse.class);
			List<Binding> candidates = response == null || response.results() == null
				? List.of()
				: response.results().bindings();
			if (candidates == null || candidates.isEmpty()) {
				return LookupResult.notFound();
			}
			if (candidates.size() != 1) {
				return LookupResult.ambiguous();
			}
			Binding candidate = candidates.get(0);
			if (candidate.artist() == null || candidate.spotifyId() == null
				|| candidate.artist().value() == null || candidate.spotifyId().value() == null) {
				return LookupResult.failed();
			}
			return LookupResult.resolved(
				entityId(candidate.artist().value()), candidate.spotifyId().value());
		} catch (RestClientException exception) {
			return LookupResult.failed();
		}
	}

	private String exactAliasQuery(String artistName) {
		return """
			PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
			PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
			PREFIX wdt: <http://www.wikidata.org/prop/direct/>
			SELECT DISTINCT ?artist ?spotifyId WHERE {
			  VALUES ?name { \"%s\"@ko }
			  { ?artist rdfs:label ?name } UNION { ?artist skos:altLabel ?name }
			  ?artist wdt:P1902 ?spotifyId .
			}
			LIMIT 2
			""".formatted(escapeSparqlLiteral(artistName));
	}

	private String escapeSparqlLiteral(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"")
			.replace("\n", "\\n").replace("\r", "\\r");
	}

	private String entityId(String entityUrl) {
		int separator = entityUrl.lastIndexOf('/');
		return separator < 0 ? entityUrl : entityUrl.substring(separator + 1);
	}

	public record LookupResult(Status status, String externalEntityId, String spotifyArtistId) {
		public static LookupResult resolved(String externalEntityId, String spotifyArtistId) {
			return new LookupResult(Status.RESOLVED, externalEntityId, spotifyArtistId);
		}

		public static LookupResult notFound() {
			return new LookupResult(Status.NOT_FOUND, null, null);
		}

		public static LookupResult ambiguous() {
			return new LookupResult(Status.AMBIGUOUS, null, null);
		}

		public static LookupResult failed() {
			return new LookupResult(Status.FAILED, null, null);
		}
	}

	public enum Status { RESOLVED, NOT_FOUND, AMBIGUOUS, FAILED }

	private record WikidataResponse(Results results) { }
	private record Results(List<Binding> bindings) { }
	private record Binding(Value artist, Value spotifyId) { }
	private record Value(String value) { }
}

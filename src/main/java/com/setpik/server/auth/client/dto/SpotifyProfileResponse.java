package com.setpik.server.auth.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotifyProfileResponse(
	@JsonProperty("account_id") String accountId,
	String id,
	String email,
	@JsonProperty("display_name") String displayName,
	List<SpotifyImageResponse> images
) {

	/** 2026년 신규 account_id를 우선 사용하고 이전 응답의 id도 호환한다. */
	public String accountIdentifier() {
		if (accountId != null && !accountId.isBlank()) {
			return accountId;
		}
		return id;
	}

	public String firstImageUrl() {
		if (images == null || images.isEmpty()) {
			return null;
		}
		return images.get(0).url();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record SpotifyImageResponse(String url) {
	}
}

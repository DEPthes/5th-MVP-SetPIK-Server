package com.setpik.server.spotify.domain;

import java.io.Serializable;
import java.util.Objects;

public class SpotifyAccountScopeId implements Serializable {

	private String scopeName;
	private Long spotifyAccountId;

	public SpotifyAccountScopeId() {
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof SpotifyAccountScopeId that)) return false;
		return Objects.equals(scopeName, that.scopeName)
			&& Objects.equals(spotifyAccountId, that.spotifyAccountId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(scopeName, spotifyAccountId);
	}
}

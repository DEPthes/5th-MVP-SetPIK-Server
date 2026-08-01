package com.setpik.server.artist.domain;

import java.io.Serializable;
import java.util.Objects;

public class ArtistGenreId implements Serializable {

	private Long artistId;
	private Long genreId;

	public ArtistGenreId() {
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof ArtistGenreId that)) return false;
		return Objects.equals(artistId, that.artistId)
			&& Objects.equals(genreId, that.genreId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(artistId, genreId);
	}
}

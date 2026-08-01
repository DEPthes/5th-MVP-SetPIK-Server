package com.setpik.server.artist.domain;

import com.setpik.server.common.domain.CreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Flyway의 Artists_Genres 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Artists_Genres")
@IdClass(ArtistGenreId.class)
public class ArtistGenre extends CreatedAtEntity {

	@Id
	@Column(name = "artist_id", nullable = false)
	private Long artistId;

	@Id
	@Column(name = "genre_id", nullable = false)
	private Long genreId;

	@Column(name = "source_type", nullable = false, length = 50)
	private String sourceType;

	protected ArtistGenre() {
	}

	public Long getArtistId() {
		return artistId;
	}

	public Long getGenreId() {
		return genreId;
	}

	public String getSourceType() {
		return sourceType;
	}

}

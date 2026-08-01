package com.setpik.server.artist.domain;

import com.setpik.server.common.domain.CreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Flyway의 Genres 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Genres")
public class Genre extends CreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "genre_id", nullable = false)
	private Long genreId;

	@Column(name = "genre_name", nullable = false, length = 255)
	private String genreName;

	@Column(name = "normalized_name", nullable = false, length = 255)
	private String normalizedName;

	protected Genre() {
	}

	public Long getGenreId() {
		return genreId;
	}

	public String getGenreName() {
		return genreName;
	}

	public String getNormalizedName() {
		return normalizedName;
	}

}

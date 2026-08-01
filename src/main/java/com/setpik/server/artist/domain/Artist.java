package com.setpik.server.artist.domain;

import com.setpik.server.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Flyway의 Artists 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Artists")
public class Artist extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "artist_id", nullable = false)
	private Long artistId;

	@Column(name = "spotify_artist_id", nullable = true, length = 255)
	private String spotifyArtistId;

	@Column(name = "kopis_artist_id", nullable = true, length = 255)
	private String kopisArtistId;

	@Column(name = "artist_name", nullable = false, length = 255)
	private String artistName;

	@Column(name = "normalized_name", nullable = false, length = 255)
	private String normalizedName;

	@Column(name = "image_url", nullable = true, length = 2048)
	private String imageUrl;

	@Column(name = "spotify_artist_url", nullable = true, length = 2048)
	private String spotifyArtistUrl;

	@Column(name = "popularity", nullable = true)
	private Short popularity;

	@Column(name = "spotify_available", nullable = false)
	private Boolean spotifyAvailable;

	protected Artist() {
	}

	public Long getArtistId() {
		return artistId;
	}

	public String getSpotifyArtistId() {
		return spotifyArtistId;
	}

	public String getKopisArtistId() {
		return kopisArtistId;
	}

	public String getArtistName() {
		return artistName;
	}

	public String getNormalizedName() {
		return normalizedName;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getSpotifyArtistUrl() {
		return spotifyArtistUrl;
	}

	public Short getPopularity() {
		return popularity;
	}

	public Boolean getSpotifyAvailable() {
		return spotifyAvailable;
	}

}

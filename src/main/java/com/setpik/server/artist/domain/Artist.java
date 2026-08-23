package com.setpik.server.artist.domain;

import com.setpik.server.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Locale;

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

	public Artist(
		String spotifyArtistId,
		String artistName,
		String spotifyArtistUrl
	) {
		this(spotifyArtistId, artistName, spotifyArtistUrl, null, null);
	}

	public Artist(
		String spotifyArtistId,
		String artistName,
		String spotifyArtistUrl,
		String imageUrl,
		Short popularity
	) {
		this.spotifyArtistId = spotifyArtistId;
		this.artistName = artistName;
		this.normalizedName = normalize(artistName);
		this.spotifyArtistUrl = spotifyArtistUrl;
		this.imageUrl = imageUrl;
		this.popularity = popularity;
		this.spotifyAvailable = true;
	}

	public static Artist fromKopis(String artistName) {
		Artist artist = new Artist();
		artist.artistName = artistName;
		artist.normalizedName = normalize(artistName);
		artist.spotifyAvailable = false;
		return artist;
	}

	/** 플레이리스트 동기화 응답에 포함된 Spotify 아티스트 정보를 갱신한다. */
	public void syncFromSpotify(
		String artistName,
		String spotifyArtistUrl,
		String imageUrl,
		Short popularity
	) {
		this.artistName = artistName;
		this.normalizedName = normalize(artistName);
		this.spotifyArtistUrl = spotifyArtistUrl;
		if (imageUrl != null) {
			this.imageUrl = imageUrl;
		}
		if (popularity != null) {
			this.popularity = popularity;
		}
		this.spotifyAvailable = true;
	}

	/** KOPIS 출연진 매칭으로 확인된 기존 Spotify 아티스트에 KOPIS ID를 연결한다. */
	public void linkKopisArtistId(String kopisArtistId) {
		if (this.kopisArtistId == null) {
			this.kopisArtistId = kopisArtistId;
		}
	}

	public static String normalize(String artistName) {
		return artistName.trim().toLowerCase(Locale.ROOT);
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

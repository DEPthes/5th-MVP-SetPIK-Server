package com.setpik.server.artist.domain;

import com.setpik.server.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Spotify_Artist_Name_Aliases")
public class SpotifyArtistNameAlias extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "name_alias_id", nullable = false)
	private Long nameAliasId;

	@Column(name = "artist_id", nullable = false)
	private Long artistId;

	@Column(name = "alias_name", nullable = false, length = 255)
	private String aliasName;

	@Column(name = "normalized_alias_name", nullable = false, length = 255)
	private String normalizedAliasName;

	@Column(name = "language_code", nullable = false, length = 10)
	private String languageCode;

	@Column(name = "source_type", nullable = false, length = 50)
	private String sourceType;

	@Column(name = "external_entity_id", length = 255)
	private String externalEntityId;

	protected SpotifyArtistNameAlias() {
	}

	private SpotifyArtistNameAlias(Long artistId, String aliasName, String normalizedAliasName,
		String languageCode, String sourceType, String externalEntityId) {
		this.artistId = artistId;
		this.aliasName = aliasName;
		this.normalizedAliasName = normalizedAliasName;
		this.languageCode = languageCode;
		this.sourceType = sourceType;
		this.externalEntityId = externalEntityId;
	}

	public static SpotifyArtistNameAlias wikidata(Long artistId, String aliasName,
		String normalizedAliasName, String externalEntityId) {
		return new SpotifyArtistNameAlias(artistId, aliasName, normalizedAliasName,
			"ko", "WIKIDATA", externalEntityId);
	}

	public Long getArtistId() {
		return artistId;
	}

	public String getAliasName() {
		return aliasName;
	}

	public String getNormalizedAliasName() {
		return normalizedAliasName;
	}
}

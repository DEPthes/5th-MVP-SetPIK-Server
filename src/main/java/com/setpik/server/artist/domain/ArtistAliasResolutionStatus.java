package com.setpik.server.artist.domain;

/** KOPIS 출연진의 외부 식별자 기반 Spotify Alias 확인 결과다. */
public enum ArtistAliasResolutionStatus {
	RESOLVED,
	NOT_FOUND,
	AMBIGUOUS,
	FAILED
}

package com.setpik.server.analysis.domain;

import java.io.Serializable;
import java.util.Objects;

public class AnalysisArtistId implements Serializable {

	private Long artistId;
	private Long analysisId;

	public AnalysisArtistId() {
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof AnalysisArtistId that)) return false;
		return Objects.equals(artistId, that.artistId)
			&& Objects.equals(analysisId, that.analysisId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(artistId, analysisId);
	}
}

package com.setpik.server.spotify.dto;

public record SpotifyScopeResponse(
	String scopeName,
	boolean isGranted
) {
}

package com.setpik.server.artist.service;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** 외부 메타데이터의 장르명을 추천에 사용하는 세부 공통 장르 코드로 변환한다. */
public final class ExternalGenreMapper {

	private ExternalGenreMapper() {
	}

	public static Set<String> toCanonicalGenres(Iterable<String> rawGenres) {
		Set<String> result = new LinkedHashSet<>();
		if (rawGenres == null) return result;
		for (String raw : rawGenres) {
			if (raw == null || raw.isBlank()) continue;
			String value = raw.toLowerCase(Locale.ROOT).trim();
			boolean kPop = containsAny(value, "k-pop", "kpop", "korean pop", "케이팝");
			if (kPop) result.add("K_POP");
			if (containsAny(value, "hip hop", "hip-hop", "hiphop", "rap", "trap", "힙합", "랩")) result.add("HIP_HOP_RAP");
			if (containsAny(value, "r&b", "rnb", "rhythm and blues", "neo soul", "soul music", "알앤비")) result.add("RNB_SOUL");
			if (containsAny(value, "indie", "인디")) result.add("INDIE");
			if (containsAny(value, "metal", "punk", "메탈", "펑크 록")) result.add("METAL_PUNK");
			if (containsAny(value, "rock", "록")) result.add("ROCK");
			if (containsAny(value, "electronic", "electronica", "edm", "house music", "techno", "일렉트로닉")) result.add("ELECTRONIC");
			if (containsAny(value, "jazz", "재즈")) result.add("JAZZ");
			if (containsAny(value, "ballad", "발라드")) result.add("BALLAD");
			if (containsAny(value, "folk", "singer-songwriter", "포크")) result.add("FOLK");
			if (containsAny(value, "classical", "orchestra", "baroque", "opera", "클래식", "오페라")) result.add("CLASSICAL");
			if (containsAny(value, "gugak", "traditional korean music", "국악")) result.add("KOREAN_TRADITIONAL");
			if (containsAny(value, "musical theatre", "musical theater", "show tunes", "뮤지컬")) result.add("MUSICAL");
			if (containsAny(value, "ballet", "contemporary dance", "modern dance", "발레", "현대무용")) result.add("DANCE_PERFORMANCE");
			if (!kPop && containsAny(value, "dance-pop", "dance pop", "pop music", "synth-pop", "pop rock", "팝 음악")) result.add("POP");
		}
		return result;
	}

	private static boolean containsAny(String value, String... keywords) {
		for (String keyword : keywords) if (value.contains(keyword)) return true;
		return false;
	}
}

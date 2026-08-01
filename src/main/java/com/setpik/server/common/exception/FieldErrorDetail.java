package com.setpik.server.common.exception;

/** 필드 검증 실패 시 명세서의 result 객체로 내려가는 상세 정보다. */
public record FieldErrorDetail(String field, String reason) {
}

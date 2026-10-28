package com.acme.hr.common.web;

import java.util.List;

/**
 * Stable API error shape used for every non-2xx JSON response, so the
 * frontend can branch on {@code code} without parsing prose messages.
 */
public record ApiError(String code, String message, List<FieldError> fieldErrors) {

	public record FieldError(String field, String message) {
	}

	public static ApiError of(String code, String message) {
		return new ApiError(code, message, List.of());
	}
}

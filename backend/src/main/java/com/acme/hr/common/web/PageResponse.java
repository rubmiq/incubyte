package com.acme.hr.common.web;

import java.util.List;
import org.springframework.data.domain.Page;

/** Stable wire shape for paginated list responses, independent of Spring Data's internal {@code Page} JSON. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(
				page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}

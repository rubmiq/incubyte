package com.acme.hr.employee.api;

import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Translates the {@code sort} query parameter into a {@link Pageable} using an explicit
 * allowlist, so client input never reaches Spring Data as a raw, unvalidated property name.
 * A secondary sort on {@code employeeCode} is always appended for deterministic ordering
 * across pages when the primary field has duplicate values.
 */
final class EmployeeSortAllowlist {

	private static final Map<String, String> SORTABLE_FIELDS = Map.of(
			"employeeCode", "employeeCode",
			"firstName", "firstName",
			"lastName", "lastName",
			"email", "email",
			"country", "country",
			"department", "department",
			"status", "status",
			"hiredOn", "hiredOn",
			"createdAt", "createdAt");

	private static final int MAX_PAGE_SIZE = 100;
	private static final int DEFAULT_PAGE_SIZE = 20;

	private EmployeeSortAllowlist() {
	}

	static Pageable toPageable(Integer page, Integer size, String sort) {
		int safePage = page == null || page < 0 ? 0 : page;
		int safeSize = size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
		return PageRequest.of(safePage, safeSize, toSort(sort));
	}

	private static Sort toSort(String sort) {
		Sort primary = parseSingleField(sort).orElse(Sort.by(Sort.Direction.ASC, "lastName"));
		Sort secondary = Sort.by(Sort.Direction.ASC, "employeeCode");
		return primary.and(secondary);
	}

	private static Optional<Sort> parseSingleField(String sort) {
		if (sort == null || sort.isBlank()) {
			return Optional.empty();
		}
		String[] parts = sort.split(",", 2);
		String property = SORTABLE_FIELDS.get(parts[0].trim());
		if (property == null) {
			return Optional.empty();
		}
		Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
				? Sort.Direction.DESC
				: Sort.Direction.ASC;
		return Optional.of(Sort.by(direction, property));
	}
}

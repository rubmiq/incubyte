package com.acme.hr.employee.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class EmployeeSortAllowlistTest {

	@Test
	void unknownSortFieldFallsBackToDefaultOrdering() {
		Pageable pageable = EmployeeSortAllowlist.toPageable(null, null, "password,asc");

		assertThat(pageable.getSort().getOrderFor("lastName")).isNotNull();
		assertThat(pageable.getSort().getOrderFor("password")).isNull();
	}

	@Test
	void allowlistedFieldAndDirectionAreHonored() {
		Pageable pageable = EmployeeSortAllowlist.toPageable(0, 10, "email,desc");

		Sort.Order order = pageable.getSort().getOrderFor("email");
		assertThat(order).isNotNull();
		assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
	}

	@Test
	void secondarySortByEmployeeCodeIsAlwaysAppendedForDeterminism() {
		Pageable pageable = EmployeeSortAllowlist.toPageable(0, 10, "department,asc");

		assertThat(pageable.getSort().getOrderFor("employeeCode")).isNotNull();
	}

	@Test
	void pageSizeIsClampedToMaximum() {
		Pageable pageable = EmployeeSortAllowlist.toPageable(0, 10_000, null);

		assertThat(pageable.getPageSize()).isEqualTo(100);
	}

	@Test
	void negativePageNumberFallsBackToZero() {
		Pageable pageable = EmployeeSortAllowlist.toPageable(-5, 10, null);

		assertThat(pageable.getPageNumber()).isZero();
	}
}

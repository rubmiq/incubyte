package com.acme.hr.analytics.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Map;

public record DashboardResponse(
		@JsonProperty("active_headcount")
		int activeHeadcount,

		@JsonProperty("total_annual_payroll_usd")
		BigDecimal totalAnnualPayrollUsd,

		@JsonProperty("mean_salary_usd")
		BigDecimal meanSalaryUsd,

		@JsonProperty("median_salary_usd")
		BigDecimal medianSalaryUsd,

		@JsonProperty("by_country")
		Map<String, CountryStats> byCountry,

		@JsonProperty("by_department")
		Map<String, DepartmentStats> byDepartment,

		@JsonProperty("fx_snapshot_date")
		String fxSnapshotDate,

		@JsonProperty("fx_snapshot_note")
		String fxSnapshotNote
) {
}

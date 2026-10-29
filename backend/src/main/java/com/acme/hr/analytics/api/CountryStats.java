package com.acme.hr.analytics.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record CountryStats(
		int headcount,

		@JsonProperty("total_payroll_usd")
		BigDecimal totalPayrollUsd,

		@JsonProperty("mean_salary_usd")
		BigDecimal meanSalaryUsd,

		@JsonProperty("median_salary_usd")
		BigDecimal medianSalaryUsd
) {
}

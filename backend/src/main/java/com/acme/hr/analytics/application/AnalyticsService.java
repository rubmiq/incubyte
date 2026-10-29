package com.acme.hr.analytics.application;

import com.acme.hr.analytics.api.CountryStats;
import com.acme.hr.analytics.api.DashboardResponse;
import com.acme.hr.analytics.api.DepartmentStats;
import com.acme.hr.common.reference.Country;
import com.acme.hr.compensation.persistence.FxRateSnapshot;
import com.acme.hr.compensation.persistence.FxRateSnapshotRepository;
import com.acme.hr.employee.persistence.Department;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Analytics service: computes dashboard metrics (headcount, payroll, mean/median
 * salaries) for the entire active employee population, and breakdowns by country
 * and department. Uses server-side SQL for efficiency and accuracy. Converts all
 * salaries to USD via the seeded demo FX snapshot.
 */
@Service
public class AnalyticsService {

	private final JdbcTemplate jdbcTemplate;
	private final FxRateSnapshotRepository fxRateSnapshotRepository;

	public AnalyticsService(JdbcTemplate jdbcTemplate, FxRateSnapshotRepository fxRateSnapshotRepository) {
		this.jdbcTemplate = jdbcTemplate;
		this.fxRateSnapshotRepository = fxRateSnapshotRepository;
	}

	public DashboardResponse computeDashboard() {
		FxRateSnapshot usdSnapshot = fxRateSnapshotRepository.findByFromCurrencyAndToCurrency("USD", "USD")
				.orElseGet(() -> {
					// Fallback if no USD-USD rate (USD is always 1:1)
					LocalDate today = LocalDate.now();
					return new FxRateSnapshot("USD", "USD", BigDecimal.ONE, today, null);
				});

		// Compute overall statistics
		int activeHeadcount = computeActiveHeadcount();
		BigDecimal totalPayrollUsd = computeTotalPayrollUsd();
		BigDecimal meanSalaryUsd = computeMeanSalaryUsd();
		BigDecimal medianSalaryUsd = computeMedianSalaryUsd();

		// Compute breakdowns by country and department
		Map<String, CountryStats> byCountry = computeByCountry();
		Map<String, DepartmentStats> byDepartment = computeByDepartment();

		LocalDate snapshotDate = usdSnapshot.getSnapshotDate();
		String snapshotNote = "Demo FX snapshot; not live rates. All salary figures in USD via snapshot dated "
				+ snapshotDate;

		return new DashboardResponse(
				activeHeadcount,
				totalPayrollUsd,
				meanSalaryUsd,
				medianSalaryUsd,
				byCountry,
				byDepartment,
				snapshotDate.toString(),
				snapshotNote
		);
	}

	private int computeActiveHeadcount() {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM employee WHERE status = 'ACTIVE'",
				Integer.class
		);
		return count != null ? count : 0;
	}

	private BigDecimal computeTotalPayrollUsd() {
		String sql = """
				SELECT COALESCE(SUM(c.amount * fx.rate), 0)
				FROM compensation c
				JOIN employee e ON c.employee_id = e.id
				JOIN fx_rate_snapshot fx ON c.currency = fx.from_currency AND fx.to_currency = 'USD'
				WHERE e.status = 'ACTIVE'
				""";
		BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class);
		return result != null ? result.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
	}

	private BigDecimal computeMeanSalaryUsd() {
		String sql = """
				SELECT COALESCE(AVG(c.amount * fx.rate), 0)
				FROM compensation c
				JOIN employee e ON c.employee_id = e.id
				JOIN fx_rate_snapshot fx ON c.currency = fx.from_currency AND fx.to_currency = 'USD'
				WHERE e.status = 'ACTIVE'
				""";
		BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class);
		return result != null ? result.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
	}

	private BigDecimal computeMedianSalaryUsd() {
		String sql = """
				SELECT COALESCE(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY c.amount * fx.rate), 0)
				FROM compensation c
				JOIN employee e ON c.employee_id = e.id
				JOIN fx_rate_snapshot fx ON c.currency = fx.from_currency AND fx.to_currency = 'USD'
				WHERE e.status = 'ACTIVE'
				""";
		BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class);
		return result != null ? result.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
	}

	private Map<String, CountryStats> computeByCountry() {
		Map<String, CountryStats> stats = new HashMap<>();

		for (Country country : Country.values()) {
			String countryCode = country.name();

			String headcountSql = "SELECT COUNT(*) FROM employee WHERE country = ? AND status = 'ACTIVE'";
			Integer headcount = jdbcTemplate.queryForObject(headcountSql, Integer.class, countryCode);

			if (headcount != null && headcount > 0) {
				String sql = """
						SELECT
						  COALESCE(SUM(c.amount * fx.rate), 0) as total_payroll,
						  COALESCE(AVG(c.amount * fx.rate), 0) as mean_salary,
						  COALESCE(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY c.amount * fx.rate), 0) as median_salary
						FROM compensation c
						JOIN employee e ON c.employee_id = e.id
						JOIN fx_rate_snapshot fx ON c.currency = fx.from_currency AND fx.to_currency = 'USD'
						WHERE e.country = ? AND e.status = 'ACTIVE'
						""";

				jdbcTemplate.query(sql, rs -> {
					if (rs.next()) {
						BigDecimal totalPayroll = rs.getBigDecimal("total_payroll").setScale(2, RoundingMode.HALF_UP);
						BigDecimal meanSalary = rs.getBigDecimal("mean_salary").setScale(2, RoundingMode.HALF_UP);
						BigDecimal medianSalary = rs.getBigDecimal("median_salary").setScale(2, RoundingMode.HALF_UP);

						stats.put(countryCode, new CountryStats(headcount, totalPayroll, meanSalary, medianSalary));
					}
				}, countryCode);
			}
		}

		return stats;
	}

	private Map<String, DepartmentStats> computeByDepartment() {
		Map<String, DepartmentStats> stats = new HashMap<>();

		for (Department department : Department.values()) {
			String deptName = department.name();

			String headcountSql = "SELECT COUNT(*) FROM employee WHERE department = ? AND status = 'ACTIVE'";
			Integer headcount = jdbcTemplate.queryForObject(headcountSql, Integer.class, deptName);

			if (headcount != null && headcount > 0) {
				String sql = """
						SELECT
						  COALESCE(SUM(c.amount * fx.rate), 0) as total_payroll,
						  COALESCE(AVG(c.amount * fx.rate), 0) as mean_salary,
						  COALESCE(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY c.amount * fx.rate), 0) as median_salary
						FROM compensation c
						JOIN employee e ON c.employee_id = e.id
						JOIN fx_rate_snapshot fx ON c.currency = fx.from_currency AND fx.to_currency = 'USD'
						WHERE e.department = ? AND e.status = 'ACTIVE'
						""";

				jdbcTemplate.query(sql, rs -> {
					if (rs.next()) {
						BigDecimal totalPayroll = rs.getBigDecimal("total_payroll").setScale(2, RoundingMode.HALF_UP);
						BigDecimal meanSalary = rs.getBigDecimal("mean_salary").setScale(2, RoundingMode.HALF_UP);
						BigDecimal medianSalary = rs.getBigDecimal("median_salary").setScale(2, RoundingMode.HALF_UP);

						stats.put(deptName, new DepartmentStats(headcount, totalPayroll, meanSalary, medianSalary));
					}
				}, deptName);
			}
		}

		return stats;
	}
}

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

	public DashboardResponse computeDashboard(String country, String department) {
		FxRateSnapshot usdSnapshot = fxRateSnapshotRepository.findByFromCurrencyAndToCurrency("USD", "USD")
				.orElseGet(() -> {
					// Fallback if no USD-USD rate (USD is always 1:1)
					LocalDate today = LocalDate.now();
					return new FxRateSnapshot("USD", "USD", BigDecimal.ONE, today, null);
				});

		// Compute overall statistics
		int activeHeadcount = computeActiveHeadcount(country, department);
		BigDecimal totalPayrollUsd = computeTotalPayrollUsd(country, department);
		BigDecimal meanSalaryUsd = computeMeanSalaryUsd(country, department);
		BigDecimal medianSalaryUsd = computeMedianSalaryUsd(country, department);

		// Compute breakdowns by country and department
		Map<String, CountryStats> byCountry = computeByCountry(country, department);
		Map<String, DepartmentStats> byDepartment = computeByDepartment(country, department);

		LocalDate snapshotDate = jdbcTemplate.queryForObject(
				"SELECT MIN(snapshot_date) FROM fx_rate_snapshot WHERE to_currency = 'USD'", LocalDate.class);
		if (snapshotDate == null) snapshotDate = usdSnapshot.getSnapshotDate();
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

	private int computeActiveHeadcount(String country, String department) {
		StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM employee e WHERE e.status = 'ACTIVE'");
		Object[] params = buildFilterParams(country, department);
		sql.append(buildFilterSql(country, department));
		Integer count = jdbcTemplate.queryForObject(sql.toString(), params, Integer.class);
		return count != null ? count : 0;
	}

	private BigDecimal computeTotalPayrollUsd(String country, String department) {
		String sql = """
				SELECT COALESCE(SUM(c.amount * CASE WHEN c.currency = 'USD' THEN 1 ELSE fx.rate END), 0)
				FROM compensation c
				JOIN employee e ON c.employee_id = e.id
				LEFT JOIN fx_rate_snapshot fx ON c.currency = fx.from_currency AND fx.to_currency = 'USD'
				WHERE e.status = 'ACTIVE'
				""" + buildFilterSql(country, department);
		Object[] params = buildFilterParams(country, department);
		BigDecimal result = jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
		return result != null ? result.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
	}

	private BigDecimal computeMeanSalaryUsd(String country, String department) {
		String sql = """
				SELECT COALESCE(AVG(c.amount * CASE WHEN c.currency = 'USD' THEN 1 ELSE fx.rate END), 0)
				FROM compensation c
				JOIN employee e ON c.employee_id = e.id
				LEFT JOIN fx_rate_snapshot fx ON c.currency = fx.from_currency AND fx.to_currency = 'USD'
				WHERE e.status = 'ACTIVE'
				""" + buildFilterSql(country, department);
		Object[] params = buildFilterParams(country, department);
		BigDecimal result = jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
		return result != null ? result.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
	}

	private BigDecimal computeMedianSalaryUsd(String country, String department) {
		String sql = """
				SELECT COALESCE(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY c.amount * CASE WHEN c.currency = 'USD' THEN 1 ELSE fx.rate END), 0)
				FROM compensation c
				JOIN employee e ON c.employee_id = e.id
				LEFT JOIN fx_rate_snapshot fx ON c.currency = fx.from_currency AND fx.to_currency = 'USD'
				WHERE e.status = 'ACTIVE'
				""" + buildFilterSql(country, department);
		Object[] params = buildFilterParams(country, department);
		BigDecimal result = jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
		return result != null ? result.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
	}

	private Map<String, CountryStats> computeByCountry(String country, String department) {
		Map<String, CountryStats> stats = new HashMap<>();

		for (Country c : Country.values()) {
			String countryCode = c.name();
			if (country != null && !country.equals(countryCode)) {
				continue;
			}

			String headcountSql = "SELECT COUNT(*) FROM employee WHERE country = ? AND status = 'ACTIVE'" +
					(department != null ? " AND department = ?" : "");
			Object[] headcountParams = department != null ? new Object[]{countryCode, department} : new Object[]{countryCode};
			Integer headcount = jdbcTemplate.queryForObject(headcountSql, headcountParams, Integer.class);

			if (headcount != null && headcount > 0) {
				String sql = """
						SELECT
						  COALESCE(SUM(c.amount * CASE WHEN c.currency = 'USD' THEN 1 ELSE fx.rate END), 0) as total_payroll,
						  COALESCE(AVG(c.amount * CASE WHEN c.currency = 'USD' THEN 1 ELSE fx.rate END), 0) as mean_salary,
						  COALESCE(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY c.amount * CASE WHEN c.currency = 'USD' THEN 1 ELSE fx.rate END), 0) as median_salary
						FROM compensation c
						JOIN employee e ON c.employee_id = e.id
						LEFT JOIN fx_rate_snapshot fx ON c.currency = fx.from_currency AND fx.to_currency = 'USD'
						WHERE e.country = ? AND e.status = 'ACTIVE'
						""" + (department != null ? " AND e.department = ?" : "");
				Object[] params = department != null ? new Object[]{countryCode, department} : new Object[]{countryCode};

				jdbcTemplate.query(sql, rs -> {
					BigDecimal totalPayroll = rs.getBigDecimal("total_payroll").setScale(2, RoundingMode.HALF_UP);
					BigDecimal meanSalary = rs.getBigDecimal("mean_salary").setScale(2, RoundingMode.HALF_UP);
					BigDecimal medianSalary = rs.getBigDecimal("median_salary").setScale(2, RoundingMode.HALF_UP);

					stats.put(countryCode, new CountryStats(headcount, totalPayroll, meanSalary, medianSalary));
				}, params);
			}
		}

		return stats;
	}

	private Map<String, DepartmentStats> computeByDepartment(String country, String department) {
		Map<String, DepartmentStats> stats = new HashMap<>();

		for (Department d : Department.values()) {
			String deptName = d.name();
			if (department != null && !department.equals(deptName)) {
				continue;
			}

			String headcountSql = "SELECT COUNT(*) FROM employee WHERE department = ? AND status = 'ACTIVE'" +
					(country != null ? " AND country = ?" : "");
			Object[] headcountParams = country != null ? new Object[]{deptName, country} : new Object[]{deptName};
			Integer headcount = jdbcTemplate.queryForObject(headcountSql, headcountParams, Integer.class);

			if (headcount != null && headcount > 0) {
				String sql = """
						SELECT
						  COALESCE(SUM(c.amount * CASE WHEN c.currency = 'USD' THEN 1 ELSE fx.rate END), 0) as total_payroll,
						  COALESCE(AVG(c.amount * CASE WHEN c.currency = 'USD' THEN 1 ELSE fx.rate END), 0) as mean_salary,
						  COALESCE(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY c.amount * CASE WHEN c.currency = 'USD' THEN 1 ELSE fx.rate END), 0) as median_salary
						FROM compensation c
						JOIN employee e ON c.employee_id = e.id
						LEFT JOIN fx_rate_snapshot fx ON c.currency = fx.from_currency AND fx.to_currency = 'USD'
						WHERE e.department = ? AND e.status = 'ACTIVE'
						""" + (country != null ? " AND e.country = ?" : "");
				Object[] params = country != null ? new Object[]{deptName, country} : new Object[]{deptName};

				jdbcTemplate.query(sql, rs -> {
					BigDecimal totalPayroll = rs.getBigDecimal("total_payroll").setScale(2, RoundingMode.HALF_UP);
					BigDecimal meanSalary = rs.getBigDecimal("mean_salary").setScale(2, RoundingMode.HALF_UP);
					BigDecimal medianSalary = rs.getBigDecimal("median_salary").setScale(2, RoundingMode.HALF_UP);

					stats.put(deptName, new DepartmentStats(headcount, totalPayroll, meanSalary, medianSalary));
				}, params);
			}
		}

		return stats;
	}

	private String buildFilterSql(String country, String department) {
		StringBuilder sb = new StringBuilder();
		if (country != null) {
			sb.append(" AND e.country = ?");
		}
		if (department != null) {
			sb.append(" AND e.department = ?");
		}
		return sb.toString();
	}

	private Object[] buildFilterParams(String country, String department) {
		if (country != null && department != null) {
			return new Object[]{country, department};
		} else if (country != null) {
			return new Object[]{country};
		} else if (department != null) {
			return new Object[]{department};
		}
		return new Object[]{};
	}
}

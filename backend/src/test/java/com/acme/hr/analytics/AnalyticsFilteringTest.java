package com.acme.hr.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.hr.TestcontainersConfiguration;
import com.acme.hr.analytics.application.AnalyticsService;
import com.acme.hr.compensation.persistence.FxRateSnapshotRepository;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AnalyticsFilteringTest.DatabaseConfiguration.class)
class AnalyticsFilteringTest {
    @TestConfiguration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "analytics.test.external-postgres", havingValue = "false", matchIfMissing = true)
    @Import(TestcontainersConfiguration.class)
    static class DatabaseConfiguration {}

    @Autowired DataSource dataSource;
    @Autowired FxRateSnapshotRepository snapshots;
    private AnalyticsService service;

    @BeforeEach
    void fixtures() {
        var jdbc = new JdbcTemplate(dataSource);
        service = new AnalyticsService(jdbc, snapshots);
        // The demo seed omits USD/USD; USD must still contribute at 1:1.
        jdbc.update("INSERT INTO fx_rate_snapshot(from_currency,to_currency,rate,snapshot_date,created_at) VALUES ('EUR','USD',2,'2026-01-01',now())");
        String[][] rows = {
            {"1", "US", "ENGINEERING", "ACTIVE", "100", "USD"},
            {"2", "US", "SALES", "ACTIVE", "300", "USD"},
            {"3", "DE", "ENGINEERING", "ACTIVE", "250", "EUR"},
            {"4", "US", "ENGINEERING", "ARCHIVED", "900", "USD"},
        };
        for (var row : rows) {
            Long id = jdbc.queryForObject("""
                INSERT INTO employee(employee_code,first_name,last_name,email,country,department,
                  job_title,status,hired_on,created_at,updated_at)
                VALUES (?, 'Test', 'Person', ?, ?, ?, 'Test', ?, '2026-01-01',now(),now()) RETURNING id
                """, Long.class, "FILTER-" + row[0], "filter" + row[0] + "@example.test", row[1], row[2], row[3]);
            jdbc.update("""
                INSERT INTO compensation(employee_id,currency,amount,effective_from,created_at,updated_at)
                VALUES (?, ?, ?, '2026-01-01',now(),now())
                """, id, row[5], new java.math.BigDecimal(row[4]));
        }
    }

    @ParameterizedTest
    @CsvSource({",,3,900,300", "US,,2,400,200", ",ENGINEERING,2,600,300",
        "US,ENGINEERING,1,100,100", "DE,SALES,0,0,0"})
    void filtersEveryAggregateAndBreakdown(String country, String department, int count,
            String total, String median) {
        var result = service.computeDashboard(country, department);
        assertThat(result.activeHeadcount()).isEqualTo(count);
        assertThat(result.totalAnnualPayrollUsd()).isEqualByComparingTo(total);
        assertThat(result.medianSalaryUsd()).isEqualByComparingTo(median);
        assertThat(result.meanSalaryUsd()).isEqualByComparingTo(median);
        assertThat(result.fxSnapshotDate()).isEqualTo("2026-01-01");
        assertThat(result.byCountry().values().stream().mapToInt(s -> s.headcount()).sum()).isEqualTo(count);
        assertThat(result.byDepartment().values().stream().mapToInt(s -> s.headcount()).sum()).isEqualTo(count);
        assertThat(result.byCountry().values().stream().map(s -> s.totalPayrollUsd())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)).isEqualByComparingTo(total);
        assertThat(result.byDepartment().values().stream().map(s -> s.totalPayrollUsd())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)).isEqualByComparingTo(total);
        if (count == 0) {
            assertThat(result.byCountry()).isEmpty();
            assertThat(result.byDepartment()).isEmpty();
        } else {
            if (country != null) assertThat(result.byCountry()).containsOnlyKeys(country);
            if (department != null) assertThat(result.byDepartment()).containsOnlyKeys(department);
        }
    }
}

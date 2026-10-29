package com.acme.hr.compensation;

import static org.assertj.core.api.Assertions.*;

import com.acme.hr.TestcontainersConfiguration;
import com.acme.hr.common.web.ConflictException;
import com.acme.hr.common.web.NotFoundException;
import com.acme.hr.common.web.RequestValidationException;
import com.acme.hr.compensation.api.*;
import com.acme.hr.compensation.application.SalaryService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SalaryService.class, SalaryServiceTest.Configuration.class, SalaryServiceTest.DatabaseConfiguration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SalaryServiceTest {
    @TestConfiguration(proxyBeanMethods = false)
    static class Configuration {
        @Bean Clock salaryTestClock() { return Clock.fixed(Instant.parse("2026-10-29T08:00:00Z"), ZoneOffset.UTC); }
        @Bean LocalValidatorFactoryBean salaryTestValidator() { return new LocalValidatorFactoryBean(); }
    }
    @TestConfiguration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "salary.test.external-postgres", havingValue = "false", matchIfMissing = true)
    @Import(TestcontainersConfiguration.class)
    static class DatabaseConfiguration {}

    @Autowired SalaryService service;
    @Autowired DataSource dataSource;
    @Value("${salary.test.external-postgres:false}") boolean external;
    JdbcTemplate jdbc;

    @BeforeEach
    void fixture() {
        jdbc = new JdbcTemplate(dataSource);
        // Never truncate application tables when using the opt-in local fallback.
        String schema = jdbc.queryForObject("SELECT current_schema()", String.class);
        if (external && (schema == null || !schema.startsWith("salary_test_"))) {
            throw new IllegalStateException("External salary tests require a dedicated salary_test_ schema.");
        }
        jdbc.execute("TRUNCATE compensation_history, compensation, employee RESTART IDENTITY CASCADE");
        jdbc.update("""
            INSERT INTO employee(employee_code,first_name,last_name,email,country,department,
              job_title,status,hired_on,created_at,updated_at)
            VALUES ('SALARY-TEST','Test','Person','salary@example.test','US','ENGINEERING',
              'Engineer','ACTIVE','2026-01-01','2026-01-01T00:00:00Z','2026-01-01T00:00:00Z')
            """);
    }

    private SaveSalaryRequest request(String amount, Long version) {
        return new SaveSalaryRequest(amount, SalaryCurrency.USD, "Annual review", version);
    }
    private long historyCount() { return jdbc.queryForObject("SELECT COUNT(*) FROM compensation_history", Long.class); }

    @Test
    void establishesAndUpdatesExactSalaryWithBeforeAfterActorAndUtcTimestamp() {
        assertThat(service.current("SALARY-TEST")).isEmpty();
        assertThat(service.history("SALARY-TEST", 0, 20).content()).isEmpty();
        var first = service.save("SALARY-TEST", request("85000.01", null), "first-hr");
        assertThat(first.amount()).isEqualTo("85000.01");
        assertThat(first.version()).isZero();
        var next = service.save("SALARY-TEST",
                new SaveSalaryRequest("90000.99", SalaryCurrency.EUR, " Promotion ", first.version()), "second-hr");
        assertThat(next.version()).isEqualTo(1);
        assertThat(next.currency()).isEqualTo(SalaryCurrency.EUR);
        assertThat(next.updatedAt()).isEqualTo(Instant.parse("2026-10-29T08:00:00Z"));
        assertThat(next.effectiveFrom().toString()).isEqualTo("2026-10-29");
        var history = service.history("SALARY-TEST", 0, 1);
        assertThat(history.totalElements()).isEqualTo(2);
        assertThat(history.totalPages()).isEqualTo(2);
        var change = history.content().getFirst();
        assertThat(change.previousAmount()).isEqualTo("85000.01");
        assertThat(change.previousCurrency()).isEqualTo("USD");
        assertThat(change.amount()).isEqualTo("90000.99");
        assertThat(change.actor()).isEqualTo("second-hr");
        assertThat(change.changeReason()).isEqualTo("Promotion");
        assertThat(change.createdAt()).isEqualTo(next.updatedAt());
        var initial = service.history("SALARY-TEST", 1, 1).content().getFirst();
        assertThat(initial.previousAmount()).isNull();
        assertThat(initial.actor()).isEqualTo("first-hr");
        assertThat(initial.id()).isLessThan(change.id());
    }

    @Test
    void rejectsStaleAndMissingVersionsWithoutHistory() {
        var first = service.save("SALARY-TEST", request("100.00", null), "hr");
        service.save("SALARY-TEST", request("200.00", first.version()), "hr");
        assertThatThrownBy(() -> service.save("SALARY-TEST", request("300.00", first.version()), "hr"))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.save("SALARY-TEST", request("300.00", null), "hr"))
                .isInstanceOf(ConflictException.class);
        assertThat(service.current("SALARY-TEST").orElseThrow().amount()).isEqualTo("200.00");
        assertThat(historyCount()).isEqualTo(2);
    }

    @Test
    void auditInsertFailureRollsBackSalaryAndVersion() {
        service.save("SALARY-TEST", request("100.00", null), "hr");
        jdbc.execute("ALTER TABLE compensation_history ADD CONSTRAINT salary_test_audit_failure CHECK (change_reason <> 'force-audit-failure')");
        try {
            assertThatThrownBy(() -> service.save("SALARY-TEST",
                    new SaveSalaryRequest("200.00", SalaryCurrency.USD, "force-audit-failure", 0L), "hr"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            var unchanged = service.current("SALARY-TEST").orElseThrow();
            assertThat(unchanged.amount()).isEqualTo("100.00");
            assertThat(unchanged.version()).isZero();
            assertThat(historyCount()).isEqualTo(1);
        } finally {
            jdbc.execute("ALTER TABLE compensation_history DROP CONSTRAINT salary_test_audit_failure");
        }
    }

    @Test
    void historyCannotBeModifiedOrDeleted() {
        service.save("SALARY-TEST", request("100.00", null), "hr");
        assertThatThrownBy(() -> jdbc.update("UPDATE compensation_history SET amount = 999"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM compensation_history"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(historyCount()).isEqualTo(1);
        assertThat(service.history("SALARY-TEST", 0, 20).content().getFirst().amount()).isEqualTo("100.00");
    }

    @Test
    void concurrentUpdatesHaveOneWinnerAndOneAuditEntry() throws Exception {
        service.save("SALARY-TEST", request("100.00", null), "hr");
        compete(0L);
        assertThat(service.current("SALARY-TEST").orElseThrow().version()).isEqualTo(1);
        assertThat(historyCount()).isEqualTo(2);
    }

    @Test
    void concurrentInitialSalariesHaveOneWinner() throws Exception {
        compete(null);
        assertThat(service.current("SALARY-TEST").orElseThrow().version()).isZero();
        assertThat(historyCount()).isEqualTo(1);
    }

    private void compete(Long version) throws Exception {
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> attempt(barrier, "200.00", version));
            var second = executor.submit(() -> attempt(barrier, "300.00", version));
            assertThat(first.get(15, TimeUnit.SECONDS) ^ second.get(15, TimeUnit.SECONDS)).isTrue();
        }
    }

    private boolean attempt(CyclicBarrier barrier, String amount, Long version) throws Exception {
        barrier.await(5, TimeUnit.SECONDS);
        try { service.save("SALARY-TEST", request(amount, version), "hr"); return true; }
        catch (ConflictException expected) { return false; }
    }

    @Test
    void rejectsInvalidMoneyReasonsAndVersionsWithoutWrites() {
        for (String amount : new String[]{"0", "-1", "1.001", "1e3", "1,000", "1000000000", " 1", "", "01.00"}) {
            assertThatThrownBy(() -> service.save("SALARY-TEST", request(amount, null), "hr"))
                    .isInstanceOf(RequestValidationException.class);
        }
        assertThatThrownBy(() -> service.save("SALARY-TEST",
                new SaveSalaryRequest("1.00", SalaryCurrency.USD, "  ", null), "hr"))
                .isInstanceOf(RequestValidationException.class);
        assertThatThrownBy(() -> service.save("SALARY-TEST", request("1.00", -1L), "hr"))
                .isInstanceOf(RequestValidationException.class);
        assertThat(service.current("SALARY-TEST")).isEmpty();
        assertThat(historyCount()).isZero();
    }

    @Test
    void rejectsUnchangedSalaryArchivedEmployeesAndMissingEmployees() {
        service.save("SALARY-TEST", request("100.00", null), "hr");
        assertThatThrownBy(() -> service.save("SALARY-TEST", request("100.00", 0L), "hr"))
                .isInstanceOf(RequestValidationException.class);
        jdbc.update("UPDATE employee SET status = 'ARCHIVED'");
        assertThatThrownBy(() -> service.save("SALARY-TEST", request("200.00", 0L), "hr"))
                .isInstanceOf(ConflictException.class);
        assertThat(service.current("SALARY-TEST").orElseThrow().amount()).isEqualTo("100.00");
        assertThatThrownBy(() -> service.current("MISSING")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.save("MISSING", request("200.00", null), "hr"))
                .isInstanceOf(NotFoundException.class);
        assertThat(historyCount()).isEqualTo(1);
    }

    @Test
    void acceptsExactAmountBoundariesWithoutRounding() {
        var first = service.save("SALARY-TEST", request("0.01", null), "hr");
        assertThat(first.amount()).isEqualTo("0.01");
        var maximum = service.save("SALARY-TEST", request("999999999.99", first.version()), "hr");
        assertThat(maximum.amount()).isEqualTo("999999999.99");
        assertThat(service.history("SALARY-TEST", 0, 20).content().getFirst().previousAmount()).isEqualTo("0.01");
    }

    @Test
    void historyIsScopedToTheRequestedEmployee() {
        jdbc.update("""
            INSERT INTO employee(employee_code,first_name,last_name,email,country,department,
              job_title,status,hired_on,created_at,updated_at)
            SELECT 'OTHER-TEST',first_name,last_name,'other@example.test',country,department,
              job_title,status,hired_on,created_at,updated_at FROM employee WHERE employee_code='SALARY-TEST'
            """);
        service.save("OTHER-TEST", request("200.00", null), "other-hr");
        assertThat(service.current("SALARY-TEST")).isEmpty();
        assertThat(service.history("SALARY-TEST", 0, 20).totalElements()).isZero();
        service.save("SALARY-TEST", request("100.00", null), "hr");
        var result = service.history("SALARY-TEST", 0, 20);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().getFirst().amount()).isEqualTo("100.00");
        assertThat(result.content().getFirst().actor()).isEqualTo("hr");
    }

    @Test
    void boundsHistoryPagination() {
        assertThatThrownBy(() -> service.history("SALARY-TEST", -1, 20)).isInstanceOf(RequestValidationException.class);
        assertThatThrownBy(() -> service.history("SALARY-TEST", 0, 101)).isInstanceOf(RequestValidationException.class);
        assertThatThrownBy(() -> service.history("SALARY-TEST", 0, 0)).isInstanceOf(RequestValidationException.class);
        assertThatThrownBy(() -> service.history("MISSING", 0, 20)).isInstanceOf(NotFoundException.class);
    }
}

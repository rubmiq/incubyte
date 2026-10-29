package com.acme.hr.compensation.application;

import com.acme.hr.common.web.*;
import com.acme.hr.compensation.api.*;
import com.acme.hr.compensation.persistence.*;
import com.acme.hr.employee.persistence.*;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SalaryService {
    private final EmployeeRepository employees;
    private final CompensationRepository salaries;
    private final CompensationHistoryRepository history;
    private final Validator validator;
    private final Clock clock;

    public SalaryService(EmployeeRepository employees, CompensationRepository salaries,
            CompensationHistoryRepository history, Validator validator, Clock clock) {
        this.employees = employees;
        this.salaries = salaries;
        this.history = history;
        this.validator = validator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<SalaryResponse> current(String code) {
        var employee = employee(code);
        return salaries.findByEmployeeId(employee.getId()).map(SalaryResponse::from);
    }

    public SalaryResponse save(String code, SaveSalaryRequest request, String actor) {
        validator.validate(request).stream().findFirst().ifPresent(violation -> {
            throw new RequestValidationException(violation.getPropertyPath().toString(), violation.getMessage());
        });
        // Locking the employee also makes initialization race-safe and serializes
        // against archive/profile writes. The version still protects stale drafts.
        var employee = employees.findByEmployeeCodeForSalaryWrite(code)
                .orElseThrow(() -> new NotFoundException("Employee not found."));
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new ConflictException("Archived employees cannot receive salary changes.");
        }
        var existing = salaries.findByEmployeeId(employee.getId());
        var amount = new BigDecimal(request.amount()).setScale(2);
        Instant now = clock.instant();
        var effectiveFrom = now.atZone(ZoneOffset.UTC).toLocalDate();
        BigDecimal previousAmount = null;
        String previousCurrency = null;
        Compensation salary;
        if (existing.isPresent()) {
            salary = existing.get();
            if (!salary.getVersion().equals(request.expectedVersion())) throw stale();
            previousAmount = salary.getAmount();
            previousCurrency = salary.getCurrency();
            if (amount.compareTo(previousAmount) == 0 && previousCurrency.equals(request.currency().name())) {
                throw new RequestValidationException("amount", "Enter a different salary amount or currency.");
            }
            salary.changeSalary(amount, request.currency().name(), effectiveFrom, now);
        } else {
            if (request.expectedVersion() != null) throw stale();
            salary = new Compensation(employee.getId(), request.currency().name(), amount, effectiveFrom, now);
        }
        // Flush before appending audit so version/constraint failures remain inside
        // this transaction; a failed audit insert rolls the salary change back.
        salary = salaries.saveAndFlush(salary);
        history.saveAndFlush(new CompensationHistory(employee.getId(), salary.getCurrency(), amount,
                request.changeReason().strip(), effectiveFrom, now, previousAmount, previousCurrency, actor));
        return SalaryResponse.from(salary);
    }

    @Transactional(readOnly = true)
    public PageResponse<SalaryHistoryEntry> history(String code, int page, int size) {
        if (page < 0) throw new RequestValidationException("page", "Page must be nonnegative.");
        if (size < 1 || size > 100) throw new RequestValidationException("size", "Size must be between 1 and 100.");
        var employee = employee(code);
        var paging = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return PageResponse.from(history.findByEmployeeId(employee.getId(), paging).map(SalaryHistoryEntry::from));
    }

    private Employee employee(String code) {
        return employees.findByEmployeeCode(code).orElseThrow(() -> new NotFoundException("Employee not found."));
    }

    private ConflictException stale() {
        return new ConflictException("Salary changed. Refresh the current salary and review your draft before saving again.");
    }
}

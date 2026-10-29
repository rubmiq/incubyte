package com.acme.hr.compensation.api;

import com.acme.hr.compensation.persistence.Compensation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

public record SalaryResponse(
        @Schema(type = "string", example = "85000.00") String amount,
        SalaryCurrency currency, long version, LocalDate effectiveFrom, Instant updatedAt) {
    public static SalaryResponse from(Compensation salary) {
        return new SalaryResponse(salary.getAmount().setScale(2).toPlainString(),
                SalaryCurrency.valueOf(salary.getCurrency()), salary.getVersion(),
                salary.getEffectiveFrom(), salary.getUpdatedAt());
    }
}

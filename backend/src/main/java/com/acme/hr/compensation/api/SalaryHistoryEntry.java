package com.acme.hr.compensation.api;

import com.acme.hr.compensation.persistence.CompensationHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

public record SalaryHistoryEntry(long id,
        @Schema(type = "string", nullable = true, description = "Previous decimal amount; null for initial/legacy records") String previousAmount,
        @Schema(nullable = true) String previousCurrency,
        @Schema(type = "string", example = "85000.00") String amount, String currency,
        @Schema(nullable = true, description = "Authenticated HR username; null when not recorded by the legacy seed") String actor,
        String changeReason,
        LocalDate effectiveFrom, Instant createdAt) {
    public static SalaryHistoryEntry from(CompensationHistory history) {
        return new SalaryHistoryEntry(history.getId(),
                history.getPreviousAmount() == null ? null : history.getPreviousAmount().setScale(2).toPlainString(),
                history.getPreviousCurrency(), history.getAmount().setScale(2).toPlainString(),
                history.getCurrency(), history.getActor(), history.getChangeReason(),
                history.getEffectiveFrom(), history.getCreatedAt());
    }
}

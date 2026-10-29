package com.acme.hr.compensation.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record SaveSalaryRequest(
        @NotBlank @Pattern(regexp = "(?:0|[1-9][0-9]{0,8})(?:\\.[0-9]{1,2})?",
                message = "Use a decimal amount with at most 9 whole and 2 fractional digits.")
        @DecimalMin("0.01") @DecimalMax("999999999.99")
        @Schema(type = "string", example = "85000.00", description = "Gross annual base salary, decimal string, 0.01–999999999.99")
        @tools.jackson.databind.annotation.JsonDeserialize(using = DecimalStringDeserializer.class)
        String amount,
        @NotNull SalaryCurrency currency,
        @NotBlank @Size(max = 500) String changeReason,
        @PositiveOrZero
        @Schema(description = "Current salary version. Null only to establish a salary when none exists.", nullable = true)
        Long expectedVersion) {}

package com.acme.hr.compensation.api;

import com.acme.hr.common.web.ApiError;
import com.acme.hr.common.web.PageResponse;
import com.acme.hr.compensation.application.SalaryService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees/{employeeCode}/salary")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Invalid fields or pagination", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "403", description = "HR permission or CSRF token required", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "404", description = "Employee not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "409", description = "Stale salary version or archived employee", content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class SalaryController {
    private final SalaryService salaries;
    public SalaryController(SalaryService salaries) { this.salaries = salaries; }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "Current annual base salary", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "204", description = "Employee has no salary yet", content = @Content)
    public ResponseEntity<SalaryResponse> current(@PathVariable String employeeCode) {
        return salaries.current(employeeCode).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping
    @ApiResponse(responseCode = "200", description = "Saved salary and new version", useReturnTypeSchema = true)
    public SalaryResponse save(@PathVariable String employeeCode, @Valid @RequestBody SaveSalaryRequest request,
            Principal principal) {
        return salaries.save(employeeCode, request, principal.getName());
    }

    @GetMapping("/history")
    @ApiResponse(responseCode = "200", description = "History page, newest UTC timestamp then ID descending", useReturnTypeSchema = true)
    public PageResponse<SalaryHistoryEntry> history(@PathVariable String employeeCode,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return salaries.history(employeeCode, page, size);
    }
}

package com.acme.hr.employee.api;

import com.acme.hr.common.reference.Country;
import com.acme.hr.common.web.PageResponse;
import com.acme.hr.common.web.ApiError;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import com.acme.hr.employee.application.EmployeeService;
import com.acme.hr.employee.persistence.Department;
import com.acme.hr.employee.persistence.Employee;
import com.acme.hr.employee.persistence.EmployeeStatus;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/employees")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "403", description = "HR permission or CSRF token required", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "404", description = "Employee not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "409", description = "Duplicate email or invalid lifecycle transition", content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class EmployeeController {

	private final EmployeeService employeeService;

	public EmployeeController(EmployeeService employeeService) {
		this.employeeService = employeeService;
	}

	@GetMapping
	public PageResponse<EmployeeSummary> list(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Country country,
			@RequestParam(required = false) Department department,
			@RequestParam(required = false) EmployeeStatus status,
			@Parameter(description = "Zero-based page; negative values become 0")
			@RequestParam(required = false) Integer page,
			@Parameter(description = "Default 20, capped at 100; non-positive values become 20")
			@RequestParam(required = false) Integer size,
			@Parameter(description = "field,asc|desc; fields: employeeCode, firstName, lastName, email, country, "
					+ "department, status, hiredOn, createdAt. Default lastName,asc. Invalid fields use default. "
					+ "employeeCode,asc is appended for deterministic ordering.")
			@RequestParam(required = false) String sort) {
		Pageable pageable = EmployeeSortAllowlist.toPageable(page, size, sort);
		Page<Employee> result = employeeService.list(search, country, department, status, pageable);
		return PageResponse.from(result.map(EmployeeSummary::from));
	}

	@GetMapping("/{employeeCode}")
	public EmployeeDetail get(@PathVariable String employeeCode) {
		return EmployeeDetail.from(employeeService.getByCode(employeeCode));
	}

	@PostMapping
	public ResponseEntity<EmployeeDetail> create(@Valid @RequestBody CreateEmployeeRequest request) {
		EmployeeDetail created = employeeService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{employeeCode}")
				.buildAndExpand(created.employeeCode())
				.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{employeeCode}")
	public EmployeeDetail update(@PathVariable String employeeCode, @Valid @RequestBody UpdateEmployeeRequest request) {
		return employeeService.update(employeeCode, request);
	}

	@PostMapping("/{employeeCode}/archive")
	public EmployeeDetail archive(@PathVariable String employeeCode) {
		return employeeService.archive(employeeCode);
	}

	@PostMapping("/{employeeCode}/reactivate")
	public EmployeeDetail reactivate(@PathVariable String employeeCode) {
		return employeeService.reactivate(employeeCode);
	}
}

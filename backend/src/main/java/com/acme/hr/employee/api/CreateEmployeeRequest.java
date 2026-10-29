package com.acme.hr.employee.api;

import com.acme.hr.common.reference.Country;
import com.acme.hr.employee.persistence.Department;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateEmployeeRequest(
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@NotBlank @Email @Size(max = 255) String email,
		@NotNull Country country,
		@NotNull Department department,
		@NotBlank @Size(max = 100) String jobTitle,
		@NotNull @PastOrPresent LocalDate hiredOn) {
}

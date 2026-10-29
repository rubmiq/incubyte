package com.acme.hr.employee.api;

import com.acme.hr.common.reference.Country;
import com.acme.hr.employee.persistence.Department;
import com.acme.hr.employee.persistence.Employee;
import com.acme.hr.employee.persistence.EmployeeStatus;
import java.time.Instant;
import java.time.LocalDate;

public record EmployeeDetail(
		String employeeCode,
		String firstName,
		String lastName,
		String email,
		Country country,
		Department department,
		String jobTitle,
		EmployeeStatus status,
		LocalDate hiredOn,
		Instant createdAt,
		Instant updatedAt) {

	public static EmployeeDetail from(Employee employee) {
		return new EmployeeDetail(
				employee.getEmployeeCode(),
				employee.getFirstName(),
				employee.getLastName(),
				employee.getEmail(),
				employee.getCountry(),
				employee.getDepartment(),
				employee.getJobTitle(),
				employee.getStatus(),
				employee.getHiredOn(),
				employee.getCreatedAt(),
				employee.getUpdatedAt());
	}
}

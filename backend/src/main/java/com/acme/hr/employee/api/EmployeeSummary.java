package com.acme.hr.employee.api;

import com.acme.hr.common.reference.Country;
import com.acme.hr.employee.persistence.Department;
import com.acme.hr.employee.persistence.Employee;
import com.acme.hr.employee.persistence.EmployeeStatus;
import java.time.LocalDate;

public record EmployeeSummary(
		String employeeCode,
		String firstName,
		String lastName,
		String email,
		Country country,
		Department department,
		String jobTitle,
		EmployeeStatus status,
		LocalDate hiredOn) {

	public static EmployeeSummary from(Employee employee) {
		return new EmployeeSummary(
				employee.getEmployeeCode(),
				employee.getFirstName(),
				employee.getLastName(),
				employee.getEmail(),
				employee.getCountry(),
				employee.getDepartment(),
				employee.getJobTitle(),
				employee.getStatus(),
				employee.getHiredOn());
	}
}

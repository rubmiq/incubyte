package com.acme.hr.employee.persistence;

import com.acme.hr.common.reference.Country;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "employee")
public class Employee {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "employee_code", nullable = false, unique = true, length = 20)
	private String employeeCode;

	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 2)
	private Country country;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private Department department;

	@Column(name = "job_title", nullable = false, length = 100)
	private String jobTitle;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EmployeeStatus status;

	@Column(name = "hired_on", nullable = false)
	private LocalDate hiredOn;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Employee() {
		// JPA
	}

	public Employee(
			String employeeCode,
			String firstName,
			String lastName,
			String email,
			Country country,
			Department department,
			String jobTitle,
			LocalDate hiredOn,
			Instant now) {
		this.employeeCode = employeeCode;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.country = country;
		this.department = department;
		this.jobTitle = jobTitle;
		this.status = EmployeeStatus.ACTIVE;
		this.hiredOn = hiredOn;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public void updateProfile(
			String firstName,
			String lastName,
			String email,
			Country country,
			Department department,
			String jobTitle,
			LocalDate hiredOn,
			Instant now) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.country = country;
		this.department = department;
		this.jobTitle = jobTitle;
		this.hiredOn = hiredOn;
		this.updatedAt = now;
	}

	public void archive(Instant now) {
		this.status = EmployeeStatus.ARCHIVED;
		this.updatedAt = now;
	}

	public void reactivate(Instant now) {
		this.status = EmployeeStatus.ACTIVE;
		this.updatedAt = now;
	}

	public Long getId() {
		return id;
	}

	public String getEmployeeCode() {
		return employeeCode;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getEmail() {
		return email;
	}

	public Country getCountry() {
		return country;
	}

	public Department getDepartment() {
		return department;
	}

	public String getJobTitle() {
		return jobTitle;
	}

	public EmployeeStatus getStatus() {
		return status;
	}

	public LocalDate getHiredOn() {
		return hiredOn;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}

package com.acme.hr.employee.persistence;

/** Employees are never hard-deleted; archiving is the only removal path. */
public enum EmployeeStatus {
	ACTIVE,
	ARCHIVED
}

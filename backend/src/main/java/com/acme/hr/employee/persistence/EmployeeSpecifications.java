package com.acme.hr.employee.persistence;

import com.acme.hr.common.reference.Country;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/** Composable filter predicates for the employee directory query. */
public final class EmployeeSpecifications {

	private EmployeeSpecifications() {
	}

	public static Specification<Employee> hasStatus(EmployeeStatus status) {
		return (root, query, cb) -> cb.equal(root.get("status"), status);
	}

	public static Specification<Employee> hasCountry(Country country) {
		return (root, query, cb) -> cb.equal(root.get("country"), country);
	}

	public static Specification<Employee> hasDepartment(Department department) {
		return (root, query, cb) -> cb.equal(root.get("department"), department);
	}

	public static Specification<Employee> matchesSearch(String term) {
		String likeTerm = "%" + term.toLowerCase() + "%";
		return (root, query, cb) -> {
			Predicate codeMatch = cb.like(cb.lower(root.get("employeeCode")), likeTerm);
			Predicate firstNameMatch = cb.like(cb.lower(root.get("firstName")), likeTerm);
			Predicate lastNameMatch = cb.like(cb.lower(root.get("lastName")), likeTerm);
			Predicate emailMatch = cb.like(cb.lower(root.get("email")), likeTerm);
			return cb.or(codeMatch, firstNameMatch, lastNameMatch, emailMatch);
		};
	}
}

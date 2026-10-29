package com.acme.hr.employee.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.hr.TestcontainersConfiguration;
import com.acme.hr.common.reference.Country;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Runs against a real Postgres (via Testcontainers), since filtering/sorting/pagination
 * behavior and the unique-constraint conflict path depend on real database semantics
 * that an in-memory substitute would not reliably reproduce.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class EmployeeRepositoryTest {

	@Autowired
	private EmployeeRepository employeeRepository;

	@BeforeEach
	void seedEmployees() {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		save("EMP-000001", "Alice", "Zimmer", "alice.zimmer@acme.test", Country.US, Department.ENGINEERING, now);
		save("EMP-000002", "Bob", "Young", "bob.young@acme.test", Country.DE, Department.SALES, now);
		save("EMP-000003", "Carol", "Young", "carol.young@acme.test", Country.US, Department.ENGINEERING, now);
	}

	private void save(
			String code, String firstName, String lastName, String email, Country country, Department department,
			Instant now) {
		employeeRepository.save(new Employee(
				code, firstName, lastName, email, country, department, "Engineer", LocalDate.of(2025, 1, 1), now));
	}

	@Test
	void findsAllEmployeesOrderedByLastNameThenEmployeeCode() {
		List<Employee> page = employeeRepository
				.findAll(org.springframework.data.jpa.domain.Specification.unrestricted(),
						PageRequest.of(0, 10, Sort.by("lastName").and(Sort.by("employeeCode"))))
				.getContent();

		assertThat(page).extracting(Employee::getEmployeeCode)
				.containsExactly("EMP-000002", "EMP-000003", "EMP-000001");
	}

	@Test
	void filtersByCountryAndDepartment() {
		var spec = EmployeeSpecifications.hasCountry(Country.US).and(EmployeeSpecifications.hasDepartment(Department.ENGINEERING));

		List<Employee> result = employeeRepository.findAll(spec, PageRequest.of(0, 10)).getContent();

		assertThat(result).extracting(Employee::getEmployeeCode)
				.containsExactlyInAnyOrder("EMP-000001", "EMP-000003");
	}

	@Test
	void searchMatchesNameEmailOrCode() {
		List<Employee> result = employeeRepository
				.findAll(EmployeeSpecifications.matchesSearch("young"), PageRequest.of(0, 10))
				.getContent();

		assertThat(result).extracting(Employee::getEmployeeCode)
				.containsExactlyInAnyOrder("EMP-000002", "EMP-000003");
	}

	@Test
	void paginatesWithDeterministicSecondOrdering() {
		var firstPage = employeeRepository.findAll(
				org.springframework.data.jpa.domain.Specification.unrestricted(),
				PageRequest.of(0, 2, Sort.by("lastName").and(Sort.by("employeeCode"))));

		assertThat(firstPage.getContent()).extracting(Employee::getEmployeeCode)
				.containsExactly("EMP-000002", "EMP-000003");
		assertThat(firstPage.getTotalElements()).isEqualTo(3);
		assertThat(firstPage.getTotalPages()).isEqualTo(2);
	}

	@Test
	void rejectsDuplicateEmail() {
		Employee duplicate = new Employee(
				"EMP-000004", "Dave", "Duplicate", "alice.zimmer@acme.test", Country.US, Department.ENGINEERING,
				"Engineer", LocalDate.of(2025, 1, 1), Instant.parse("2026-01-01T00:00:00Z"));

		org.junit.jupiter.api.Assertions.assertThrows(
				DataIntegrityViolationException.class, () -> employeeRepository.saveAndFlush(duplicate));
	}

	@Test
	void sequenceProducesIncreasingValues() {
		long first = employeeRepository.nextEmployeeCodeSequence();
		long second = employeeRepository.nextEmployeeCodeSequence();

		assertThat(second).isGreaterThan(first);
	}
}

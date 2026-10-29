package com.acme.hr.employee.application;

import com.acme.hr.common.reference.Country;
import com.acme.hr.common.web.ConflictException;
import com.acme.hr.common.web.NotFoundException;
import com.acme.hr.employee.api.CreateEmployeeRequest;
import com.acme.hr.employee.api.EmployeeDetail;
import com.acme.hr.employee.api.UpdateEmployeeRequest;
import com.acme.hr.employee.persistence.Department;
import com.acme.hr.employee.persistence.Employee;
import com.acme.hr.employee.persistence.EmployeeRepository;
import com.acme.hr.employee.persistence.EmployeeSpecifications;
import com.acme.hr.employee.persistence.EmployeeStatus;
import java.time.Clock;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmployeeService {

	private final EmployeeRepository employeeRepository;
	private final Clock clock;

	public EmployeeService(EmployeeRepository employeeRepository, Clock clock) {
		this.employeeRepository = employeeRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public Page<Employee> list(
			String search, Country country, Department department, EmployeeStatus status, Pageable pageable) {
		Specification<Employee> spec = Specification.unrestricted();
		if (search != null && !search.isBlank()) {
			spec = spec.and(EmployeeSpecifications.matchesSearch(search));
		}
		if (country != null) {
			spec = spec.and(EmployeeSpecifications.hasCountry(country));
		}
		if (department != null) {
			spec = spec.and(EmployeeSpecifications.hasDepartment(department));
		}
		if (status != null) {
			spec = spec.and(EmployeeSpecifications.hasStatus(status));
		}
		return employeeRepository.findAll(spec, pageable);
	}

	@Transactional(readOnly = true)
	public Employee getByCode(String employeeCode) {
		return employeeRepository
				.findByEmployeeCode(employeeCode)
				.orElseThrow(() -> new NotFoundException("No employee found with code " + employeeCode + "."));
	}

	public EmployeeDetail create(CreateEmployeeRequest request) {
		String employeeCode = "EMP-%06d".formatted(employeeRepository.nextEmployeeCodeSequence());
		Instant now = Instant.now(clock);
		Employee employee = new Employee(
				employeeCode,
				request.firstName(),
				request.lastName(),
				request.email(),
				request.country(),
				request.department(),
				request.jobTitle(),
				request.hiredOn(),
				now);
		return EmployeeDetail.from(employeeRepository.save(employee));
	}

	public EmployeeDetail update(String employeeCode, UpdateEmployeeRequest request) {
		Employee employee = getByCode(employeeCode);
		employee.updateProfile(
				request.firstName(),
				request.lastName(),
				request.email(),
				request.country(),
				request.department(),
				request.jobTitle(),
				request.hiredOn(),
				Instant.now(clock));
		return EmployeeDetail.from(employee);
	}

	public EmployeeDetail archive(String employeeCode) {
		Employee employee = getByCode(employeeCode);
		if (employee.getStatus() == EmployeeStatus.ARCHIVED) {
			throw new ConflictException("Employee " + employeeCode + " is already archived.");
		}
		employee.archive(Instant.now(clock));
		return EmployeeDetail.from(employee);
	}

	public EmployeeDetail reactivate(String employeeCode) {
		Employee employee = getByCode(employeeCode);
		if (employee.getStatus() == EmployeeStatus.ACTIVE) {
			throw new ConflictException("Employee " + employeeCode + " is already active.");
		}
		employee.reactivate(Instant.now(clock));
		return EmployeeDetail.from(employee);
	}
}

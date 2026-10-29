package com.acme.hr.employee.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

	Optional<Employee> findByEmployeeCode(String employeeCode);

	@Query(value = "SELECT nextval('employee_code_seq')", nativeQuery = true)
	long nextEmployeeCodeSequence();
}

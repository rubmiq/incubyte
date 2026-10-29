package com.acme.hr.employee.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

	Optional<Employee> findByEmployeeCode(String employeeCode);

	@org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from Employee e where e.employeeCode = :employeeCode")
	Optional<Employee> findByEmployeeCodeForSalaryWrite(String employeeCode);

	@Query(value = "SELECT nextval('employee_code_seq')", nativeQuery = true)
	long nextEmployeeCodeSequence();
}

package com.acme.hr.compensation.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompensationRepository extends JpaRepository<Compensation, Long> {
	Optional<Compensation> findByEmployeeId(Long employeeId);
}

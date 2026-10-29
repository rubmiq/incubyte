package com.acme.hr.compensation.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompensationHistoryRepository extends JpaRepository<CompensationHistory, Long> {
	Page<CompensationHistory> findByEmployeeId(Long employeeId, Pageable pageable);
}

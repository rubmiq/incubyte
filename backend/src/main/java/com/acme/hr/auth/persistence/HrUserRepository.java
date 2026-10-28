package com.acme.hr.auth.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HrUserRepository extends JpaRepository<HrUser, Long> {

	Optional<HrUser> findByUsername(String username);
}

package com.acme.hr.compensation.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FxRateSnapshotRepository extends JpaRepository<FxRateSnapshot, Long> {
	Optional<FxRateSnapshot> findByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);
}

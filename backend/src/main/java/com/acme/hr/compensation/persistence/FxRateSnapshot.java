package com.acme.hr.compensation.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "fx_rate_snapshot")
public class FxRateSnapshot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "from_currency", nullable = false, length = 3)
	private String fromCurrency;

	@Column(name = "to_currency", nullable = false, length = 3)
	private String toCurrency;

	@Column(nullable = false)
	private BigDecimal rate;

	@Column(name = "snapshot_date", nullable = false)
	private LocalDate snapshotDate;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	public FxRateSnapshot() {
	}

	public FxRateSnapshot(String fromCurrency, String toCurrency, BigDecimal rate, LocalDate snapshotDate,
			Instant now) {
		this.fromCurrency = fromCurrency;
		this.toCurrency = toCurrency;
		this.rate = rate;
		this.snapshotDate = snapshotDate;
		this.createdAt = now;
	}

	public Long getId() {
		return id;
	}

	public String getFromCurrency() {
		return fromCurrency;
	}

	public String getToCurrency() {
		return toCurrency;
	}

	public BigDecimal getRate() {
		return rate;
	}

	public LocalDate getSnapshotDate() {
		return snapshotDate;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

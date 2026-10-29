package com.acme.hr.compensation.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "compensation")
public class Compensation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "employee_id", nullable = false)
	private Long employeeId;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(nullable = false)
	private BigDecimal amount;

	@Version
	private Long version;

	@Column(name = "effective_from", nullable = false)
	private LocalDate effectiveFrom;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public Compensation() {
	}

	public Compensation(Long employeeId, String currency, BigDecimal amount, LocalDate effectiveFrom,
			Instant now) {
		this.employeeId = employeeId;
		this.currency = currency;
		this.amount = amount;
		this.effectiveFrom = effectiveFrom;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public Long getId() {
		return id;
	}

	public void changeSalary(BigDecimal amount, String currency, LocalDate effectiveFrom, Instant now) {
		this.amount = amount;
		this.currency = currency;
		this.effectiveFrom = effectiveFrom;
		this.updatedAt = now;
	}

	public Long getEmployeeId() {
		return employeeId;
	}

	public String getCurrency() {
		return currency;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public Long getVersion() {
		return version;
	}

	public LocalDate getEffectiveFrom() {
		return effectiveFrom;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}

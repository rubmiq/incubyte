package com.acme.hr.compensation.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "compensation_history")
public class CompensationHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "employee_id", nullable = false)
	private Long employeeId;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(nullable = false)
	private BigDecimal amount;

	@Column(name = "change_reason", nullable = false, length = 500)
	private String changeReason;

	@Column(name = "effective_from", nullable = false)
	private LocalDate effectiveFrom;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "previous_amount")
	private BigDecimal previousAmount;

	@Column(name = "previous_currency", length = 3)
	private String previousCurrency;

	@Column(length = 255)
	private String actor;

	public CompensationHistory() {
	}

	public CompensationHistory(Long employeeId, String currency, BigDecimal amount, String changeReason,
			LocalDate effectiveFrom, Instant now) {
		this.employeeId = employeeId;
		this.currency = currency;
		this.amount = amount;
		this.changeReason = changeReason;
		this.effectiveFrom = effectiveFrom;
		this.createdAt = now;
	}

	public Long getId() {
		return id;
	}

	public CompensationHistory(Long employeeId, String currency, BigDecimal amount, String changeReason,
			LocalDate effectiveFrom, Instant now, BigDecimal previousAmount, String previousCurrency, String actor) {
		this(employeeId, currency, amount, changeReason, effectiveFrom, now);
		this.previousAmount = previousAmount;
		this.previousCurrency = previousCurrency;
		this.actor = actor;
	}

	public BigDecimal getPreviousAmount() { return previousAmount; }
	public String getPreviousCurrency() { return previousCurrency; }
	public String getActor() { return actor; }

	public Long getEmployeeId() {
		return employeeId;
	}

	public String getCurrency() {
		return currency;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getChangeReason() {
		return changeReason;
	}

	public LocalDate getEffectiveFrom() {
		return effectiveFrom;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

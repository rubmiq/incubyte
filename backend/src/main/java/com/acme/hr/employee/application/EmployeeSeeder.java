package com.acme.hr.employee.application;

import com.acme.hr.common.reference.Country;
import com.acme.hr.compensation.persistence.Compensation;
import com.acme.hr.compensation.persistence.CompensationHistory;
import com.acme.hr.compensation.persistence.CompensationHistoryRepository;
import com.acme.hr.compensation.persistence.CompensationRepository;
import com.acme.hr.compensation.persistence.FxRateSnapshot;
import com.acme.hr.compensation.persistence.FxRateSnapshotRepository;
import com.acme.hr.employee.persistence.Department;
import com.acme.hr.employee.persistence.Employee;
import com.acme.hr.employee.persistence.EmployeeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates exactly 10,000 deterministic synthetic employees with compensation
 * history. Uses a seeded RNG and fixed clock for reproducibility. Distributions
 * are meaningful: countries, departments, currencies and salary ranges reflect
 * the reference data and business rules.
 */
@Service
public class EmployeeSeeder {

	private static final int TARGET_EMPLOYEE_COUNT = 10_000;
	private static final long SEED = 42L; // Fixed seed for reproducibility
	private static final LocalDate SNAPSHOT_DATE = LocalDate.of(2026, 10, 29);
	private static final LocalDate SEED_EFFECTIVE_DATE = LocalDate.of(2026, 1, 1);

	private final EmployeeRepository employeeRepository;
	private final CompensationRepository compensationRepository;
	private final CompensationHistoryRepository compensationHistoryRepository;
	private final FxRateSnapshotRepository fxRateSnapshotRepository;
	private final Clock clock;

	public EmployeeSeeder(EmployeeRepository employeeRepository, CompensationRepository compensationRepository,
			CompensationHistoryRepository compensationHistoryRepository, FxRateSnapshotRepository fxRateSnapshotRepository,
			Clock clock) {
		this.employeeRepository = employeeRepository;
		this.compensationRepository = compensationRepository;
		this.compensationHistoryRepository = compensationHistoryRepository;
		this.fxRateSnapshotRepository = fxRateSnapshotRepository;
		this.clock = clock;
	}

	@Transactional
	public void seedEmployees() {
		// Idempotent: if already seeded, don't re-seed
		long existingCount = employeeRepository.count();
		if (existingCount > 0) {
			return;
		}

		Random rng = new Random(SEED);

		// Seed FX rates first (demo only, labeled snapshot)
		seedFxRates();

		// Generate and persist employees with compensation
		for (int i = 0; i < TARGET_EMPLOYEE_COUNT; i++) {
			Employee employee = generateDeterministicEmployee(i, rng);
			Employee saved = employeeRepository.save(employee);

			// Create initial compensation and history
			String currency = employee.getCountry().currencyCode();
			BigDecimal salary = generateSalaryRange(employee.getDepartment(), rng, currency);

			Compensation compensation = new Compensation(saved.getId(), currency, salary, SEED_EFFECTIVE_DATE,
					Instant.now(clock));
			compensationRepository.save(compensation);

			CompensationHistory history = new CompensationHistory(saved.getId(), currency, salary, "Initial seeded salary",
					SEED_EFFECTIVE_DATE, Instant.now(clock));
			compensationHistoryRepository.save(history);
		}
	}

	private Employee generateDeterministicEmployee(int index, Random rng) {
		String employeeCode = String.format("EMP%06d", index + 1);
		String[] firstNames = { "Alice", "Bob", "Charlie", "Diana", "Edward", "Fiona", "George", "Hannah", "Ivan",
				"Julia", "Kevin", "Laura", "Michael", "Nancy", "Oscar" };
		String[] lastNames = { "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
				"Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson" };
		String firstName = firstNames[rng.nextInt(firstNames.length)];
		String lastName = lastNames[rng.nextInt(lastNames.length)];

		Country[] countries = Country.values();
		Country country = countries[rng.nextInt(countries.length)];

		Department[] departments = Department.values();
		Department department = departments[rng.nextInt(departments.length)];

		String email = String.format("%s.%s.%06d@acme.local", firstName.toLowerCase(), lastName.toLowerCase(),
				index + 1);

		LocalDate hiredOn = LocalDate.of(2020 + rng.nextInt(5), rng.nextInt(12) + 1, rng.nextInt(28) + 1);

		String[] jobTitles = { "Engineer", "Manager", "Analyst", "Designer", "Specialist", "Lead", "Coordinator",
				"Director", "Officer", "Associate" };
		String jobTitle = jobTitles[rng.nextInt(jobTitles.length)] + " - " + department;

		Instant now = Instant.now(clock);
		return new Employee(employeeCode, firstName, lastName, email, country, department, jobTitle, hiredOn, now);
	}

	private BigDecimal generateSalaryRange(Department department, Random rng, String currency) {
		int baseMin = 40_000;
		int baseMax = 150_000;

		switch (department) {
			case ENGINEERING:
				baseMin = 80_000;
				baseMax = 180_000;
				break;
			case SALES:
				baseMin = 60_000;
				baseMax = 140_000;
				break;
			case HUMAN_RESOURCES:
				baseMin = 50_000;
				baseMax = 110_000;
				break;
			case OPERATIONS:
				baseMin = 55_000;
				baseMax = 130_000;
				break;
			case FINANCE:
				baseMin = 70_000;
				baseMax = 160_000;
				break;
			default:
				baseMin = 45_000;
				baseMax = 135_000;
		}

		// Generate salary and apply currency multiplier
		int salary = baseMin + rng.nextInt(baseMax - baseMin + 1);
		BigDecimal multiplier = getLocalSalaryMultiplier(currency);

		return BigDecimal.valueOf(salary).multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal getLocalSalaryMultiplier(String currency) {
		Map<String, BigDecimal> multipliers = new HashMap<>();
		multipliers.put("USD", BigDecimal.ONE);
		multipliers.put("GBP", new BigDecimal("1.25"));
		multipliers.put("EUR", new BigDecimal("1.15"));
		multipliers.put("CAD", new BigDecimal("0.75"));
		multipliers.put("AUD", new BigDecimal("0.65"));
		multipliers.put("JPY", new BigDecimal("0.0067"));
		multipliers.put("INR", new BigDecimal("0.012"));
		multipliers.put("BRL", new BigDecimal("0.20"));
		multipliers.put("MXN", new BigDecimal("0.058"));
		multipliers.put("SGD", new BigDecimal("0.74"));

		return multipliers.getOrDefault(currency, BigDecimal.ONE);
	}

	private void seedFxRates() {
		Map<String, BigDecimal> ratesUsd = new HashMap<>();
		ratesUsd.put("USD", BigDecimal.ONE);
		ratesUsd.put("GBP", new BigDecimal("1.27"));
		ratesUsd.put("EUR", new BigDecimal("1.10"));
		ratesUsd.put("CAD", new BigDecimal("0.74"));
		ratesUsd.put("AUD", new BigDecimal("0.67"));
		ratesUsd.put("JPY", new BigDecimal("0.0067"));
		ratesUsd.put("INR", new BigDecimal("0.012"));
		ratesUsd.put("BRL", new BigDecimal("0.20"));
		ratesUsd.put("MXN", new BigDecimal("0.059"));
		ratesUsd.put("SGD", new BigDecimal("0.74"));

		Instant now = Instant.now(clock);
		ratesUsd.forEach((currency, rate) -> {
			if (!currency.equals("USD")) {
				FxRateSnapshot snapshot = new FxRateSnapshot(currency, "USD", rate, SNAPSHOT_DATE, now);
				fxRateSnapshotRepository.save(snapshot);
			}
		});
	}
}

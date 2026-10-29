package com.acme.hr.employee.application;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Explicit, opt-in seed command runner. Seeding is never automatic; it runs
 * only when explicitly invoked via the `--seed` argument.
 */
@Component
public class SeedCommand implements ApplicationRunner {

	private final EmployeeSeeder employeeSeeder;

	public SeedCommand(EmployeeSeeder employeeSeeder) {
		this.employeeSeeder = employeeSeeder;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (args.containsOption("seed")) {
			System.out.println("🌱 Starting deterministic seed of 10,000 synthetic employees...");
			employeeSeeder.seedEmployees();
			System.out.println("✓ Seeding complete. Database now contains 10,000 employees with compensation history.");
		}
	}
}

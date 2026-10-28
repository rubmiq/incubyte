package com.acme.hr.auth.application;

import com.acme.hr.auth.config.DemoHrProperties;
import com.acme.hr.auth.persistence.HrUser;
import com.acme.hr.auth.persistence.HrUserRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures the single environment-configured demo HR account exists on every
 * boot. Deliberately separate from the (much larger, explicitly opt-in)
 * 10,000-employee seed: this is an idempotent login-account bootstrap that
 * must always run, not a bulk data operation that must never run by
 * accident.
 */
@Component
@EnableConfigurationProperties(DemoHrProperties.class)
public class DemoHrUserInitializer implements ApplicationRunner {

	private final HrUserRepository hrUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final DemoHrProperties demoHrProperties;
	private final Clock clock;

	public DemoHrUserInitializer(
			HrUserRepository hrUserRepository,
			PasswordEncoder passwordEncoder,
			DemoHrProperties demoHrProperties,
			Clock clock) {
		this.hrUserRepository = hrUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.demoHrProperties = demoHrProperties;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		String username = demoHrProperties.hrUsername();
		String rawPassword = demoHrProperties.hrPassword();

		hrUserRepository.findByUsername(username).ifPresentOrElse(
				existing -> {
					if (!passwordEncoder.matches(rawPassword, existing.getPasswordHash())) {
						existing.setPasswordHash(passwordEncoder.encode(rawPassword));
					}
				},
				() -> hrUserRepository.save(
						new HrUser(username, passwordEncoder.encode(rawPassword), Instant.now(clock))));
	}
}

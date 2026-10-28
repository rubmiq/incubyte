package com.acme.hr;

import org.springframework.boot.SpringApplication;

public class TestSalaryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(SalaryServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

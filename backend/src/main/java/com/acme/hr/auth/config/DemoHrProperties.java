package com.acme.hr.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code app.demo.hr-username} / {@code app.demo.hr-password} (always environment-provided, no default). */
@ConfigurationProperties(prefix = "app.demo")
public record DemoHrProperties(String hrUsername, String hrPassword) {
}

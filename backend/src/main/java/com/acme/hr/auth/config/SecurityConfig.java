package com.acme.hr.auth.config;

import com.acme.hr.auth.application.HrUserDetailsService;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.http.HttpStatus;

/**
 * Session-based auth for the single HR role, same-origin, no CORS, no
 * bearer tokens. CSRF wiring follows the official Spring Security 7 SPA
 * guidance (docs.spring.io/spring-security/reference/servlet/exploits/csrf.html):
 * {@code csrf.spa()} wires an HttpOnly=false cookie repository plus a
 * BREACH-safe deferred token handler, and relies on a GET endpoint
 * ({@code /api/auth/csrf}) to force the cookie to be (re)issued on initial
 * load and after login/logout, since the cookie is cleared on both events.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(
			HrUserDetailsService hrUserDetailsService, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(hrUserDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new org.springframework.security.authentication.ProviderManager(provider);
	}

	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http, SecurityContextRepository securityContextRepository) throws Exception {
		http
				.csrf(csrf -> csrf.spa())
				.securityContext(context -> context.securityContextRepository(securityContextRepository))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/api/auth/login", "/api/auth/csrf").permitAll()
						.requestMatchers("/actuator/health").permitAll()
						.anyRequest().hasRole("HR"))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(jsonEntryPoint())
						.accessDeniedHandler(jsonAccessDeniedHandler()))
				.logout(logout -> logout
						.logoutUrl("/api/auth/logout")
						.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
						.deleteCookies("JSESSIONID"));
		return http.build();
	}

	private org.springframework.security.web.AuthenticationEntryPoint jsonEntryPoint() {
		return (request, response, authException) -> writeJsonError(
				response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is required.");
	}

	private org.springframework.security.web.access.AccessDeniedHandler jsonAccessDeniedHandler() {
		return (request, response, accessDeniedException) -> writeJsonError(
				response, HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have access to this resource.");
	}

	/**
	 * Written by hand (not via the MVC-layer {@code ObjectMapper}) because
	 * this runs at the servlet-filter level, before and independent of
	 * Spring MVC dispatch — {@code code}/{@code message} here are always
	 * static constants, never interpolated request data, so no escaping is
	 * needed.
	 */
	private void writeJsonError(
			jakarta.servlet.http.HttpServletResponse response, HttpStatus status, String code, String message)
			throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write(
				"{\"code\":\"%s\",\"message\":\"%s\",\"fieldErrors\":[]}".formatted(code, message));
	}
}


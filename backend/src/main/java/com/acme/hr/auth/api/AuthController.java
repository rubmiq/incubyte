package com.acme.hr.auth.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login/me/csrf endpoints. Logout is handled declaratively by Spring
 * Security's logout filter (configured in {@code SecurityConfig}), not
 * here, so session invalidation and CSRF-cookie clearing stay consistent
 * with the framework's own exploit protections.
 */
@RestController
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;

	public AuthController(AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository) {
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
	}

	@PostMapping("/api/auth/login")
	@SecurityRequirements
	public CurrentUserResponse login(
			@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		Authentication authenticated = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authenticated);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, httpRequest, httpResponse);

		return toResponse(authenticated);
	}

	@GetMapping("/api/auth/me")
	public ResponseEntity<CurrentUserResponse> me(Authentication authentication) {
		if (authentication == null) {
			return ResponseEntity.status(401).build();
		}
		return ResponseEntity.ok(toResponse(authentication));
	}

	/**
	 * Forces the XSRF-TOKEN cookie to be (re)issued: the deferred token
	 * wired by {@code csrf.spa()} is only written to the response when
	 * something actually resolves it, and the cookie is cleared after
	 * login/logout, so the SPA calls this once on startup and again after
	 * both of those events.
	 */
	@GetMapping("/api/auth/csrf")
	@SecurityRequirements
	public void csrf(CsrfToken csrfToken) {
		// Resolving the CsrfToken parameter is sufficient to trigger the cookie write.
	}

	private CurrentUserResponse toResponse(Authentication authentication) {
		return new CurrentUserResponse(authentication.getName(), "HR");
	}
}

package com.acme.hr.common.web;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions thrown by controllers/services into the shared
 * {@link ApiError} shape. Spring MVC's {@code @ExceptionHandler} advice
 * intercepts exceptions during dispatch before they can reach the security
 * filter chain's {@code ExceptionTranslationFilter}, so an
 * {@link AuthenticationException} thrown directly from a controller (e.g.
 * the login endpoint's explicit {@code authenticationManager.authenticate()}
 * call) must be mapped here too, not only in {@code SecurityConfig}'s
 * filter-level entry point.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ApiError> handleAuthenticationFailure(AuthenticationException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiError.of("UNAUTHENTICATED", "Invalid username or password."));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
		List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
				.toList();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiError("VALIDATION_FAILED", "One or more fields are invalid.", fieldErrors));
	}

	@ExceptionHandler(NotFoundException.class)
	ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiError.of("NOT_FOUND", ex.getMessage()));
	}

	@ExceptionHandler(ConflictException.class)
	ResponseEntity<ApiError> handleConflict(ConflictException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiError.of("CONFLICT", ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> handleUnexpected(Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiError.of("INTERNAL_ERROR", "An unexpected error occurred."));
	}
}

package com.acme.hr.common.web;

/** Thrown when a requested domain resource does not exist (or is not visible to the caller). */
public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}
}

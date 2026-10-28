package com.acme.hr.common.web;

/** Thrown on a stale write (e.g. optimistic-lock version mismatch); maps to HTTP 409. */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}

package com.acme.hr.common.reference;

/**
 * Fixed allowlist of countries the demo dataset spans, each tied to its
 * local reporting currency. Static reference data, not a database table,
 * per the project's decision to keep currency metadata as application
 * config rather than user-editable rows.
 */
public enum Country {

	US("United States", "USD"),
	GB("United Kingdom", "GBP"),
	DE("Germany", "EUR"),
	FR("France", "EUR"),
	NL("Netherlands", "EUR"),
	IN("India", "INR"),
	BR("Brazil", "BRL"),
	AU("Australia", "AUD"),
	CA("Canada", "CAD"),
	SG("Singapore", "SGD");

	private final String displayName;
	private final String currencyCode;

	Country(String displayName, String currencyCode) {
		this.displayName = displayName;
		this.currencyCode = currencyCode;
	}

	public String displayName() {
		return displayName;
	}

	public String currencyCode() {
		return currencyCode;
	}
}

package com.acme.hr.common.web;

public class RequestValidationException extends RuntimeException {
    private final String field;
    public RequestValidationException(String field, String message) {
        super(message);
        this.field = field;
    }
    public String field() { return field; }
}

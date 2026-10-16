package com.hotel.reservations.common.exception;

/** Thrown when an operation violates a business rule (HTTP 409). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}

package com.hotel.reservations.common.exception;

/** Thrown when an authenticated user tries to touch a resource owned by someone else (HTTP 403). */
public class AccessDeniedBusinessException extends RuntimeException {

    public AccessDeniedBusinessException(String message) {
        super(message);
    }
}

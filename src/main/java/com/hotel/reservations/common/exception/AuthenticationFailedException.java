package com.hotel.reservations.common.exception;

/** Thrown when credentials or tokens are invalid (HTTP 401). */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}

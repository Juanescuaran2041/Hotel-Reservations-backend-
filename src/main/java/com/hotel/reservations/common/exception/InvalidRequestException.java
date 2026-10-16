package com.hotel.reservations.common.exception;

/** Thrown when request data is semantically invalid (HTTP 400). */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}

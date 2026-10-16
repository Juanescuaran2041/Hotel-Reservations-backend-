package com.hotel.reservations.hotel.model;

import java.time.Instant;

public record DigitalKey(String code, String roomNumber, Instant issuedAt) {
}

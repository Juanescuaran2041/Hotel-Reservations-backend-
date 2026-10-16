package com.hotel.reservations.hotel.dto;

import java.time.Instant;

public record DigitalKeyResponse(String code, String roomNumber, Instant issuedAt) {
}

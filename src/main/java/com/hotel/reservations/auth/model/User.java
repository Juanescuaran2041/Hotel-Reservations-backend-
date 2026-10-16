package com.hotel.reservations.auth.model;

import java.util.UUID;

/** Registered account. Only the BCrypt hash of the password is ever stored. */
public record User(UUID id, String fullName, String email, String passwordHash, Role role) {
}

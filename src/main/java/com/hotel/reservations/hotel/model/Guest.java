package com.hotel.reservations.hotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record Guest(
        @Column(name = "guest_name", nullable = false) String fullName,
        @Column(name = "guest_email", nullable = false) String email,
        @Column(name = "guest_phone") String phone) {
}

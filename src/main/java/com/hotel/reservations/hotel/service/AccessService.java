package com.hotel.reservations.hotel.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.hotel.reservations.hotel.model.DigitalKey;
import com.hotel.reservations.hotel.model.Reservation;
import com.hotel.reservations.hotel.model.ReservationStatus;
import com.hotel.reservations.hotel.repository.ReservationRepository;

/** Subsystem: simulated digital room keys. A key is valid exactly while its reservation is checked in. */
@Service
public class AccessService {

    private final ReservationRepository reservationRepository;

    public AccessService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public DigitalKey issueKey(Reservation reservation) {
        return new DigitalKey(UUID.randomUUID().toString(), reservation.getRoom().getNumber(), Instant.now());
    }

    public boolean isValid(String code) {
        return reservationRepository.existsByDigitalKeyCodeAndStatus(code, ReservationStatus.CHECKED_IN);
    }
}

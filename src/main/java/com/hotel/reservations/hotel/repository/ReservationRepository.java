package com.hotel.reservations.hotel.repository;

import java.util.Optional;
import java.util.UUID;

import com.hotel.reservations.hotel.model.Reservation;

public interface ReservationRepository {

    Reservation save(Reservation reservation);

    Optional<Reservation> findById(UUID id);
}

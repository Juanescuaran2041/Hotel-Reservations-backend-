package com.hotel.reservations.hotel.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hotel.reservations.hotel.model.Reservation;
import com.hotel.reservations.hotel.model.ReservationStatus;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    boolean existsByDigitalKeyCodeAndStatus(String digitalKeyCode, ReservationStatus status);
}

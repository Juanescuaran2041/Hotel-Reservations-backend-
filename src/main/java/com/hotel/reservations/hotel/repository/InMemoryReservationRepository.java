package com.hotel.reservations.hotel.repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.hotel.reservations.hotel.model.Reservation;

@Repository
public class InMemoryReservationRepository implements ReservationRepository {

    private final Map<UUID, Reservation> store = new ConcurrentHashMap<>();

    @Override
    public Reservation save(Reservation reservation) {
        store.put(reservation.getId(), reservation);
        return reservation;
    }

    @Override
    public Optional<Reservation> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }
}

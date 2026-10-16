package com.hotel.reservations.hotel.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A hotel room. Its bookings are guarded by the monitor of the room itself. */
public class Room {

    private final String number;
    private final RoomType type;
    private final List<Booking> bookings = new ArrayList<>();

    public Room(String number, RoomType type) {
        this.number = number;
        this.type = type;
    }

    public String getNumber() {
        return number;
    }

    public RoomType getType() {
        return type;
    }

    public synchronized boolean isAvailable(LocalDate from, LocalDate to) {
        return bookings.stream().noneMatch(booking -> booking.overlaps(from, to));
    }

    /** Atomically books the room; returns false when the range is already taken. */
    public synchronized boolean book(UUID reservationId, LocalDate from, LocalDate to) {
        if (!isAvailable(from, to)) {
            return false;
        }
        bookings.add(new Booking(reservationId, from, to));
        return true;
    }

    public synchronized void release(UUID reservationId) {
        bookings.removeIf(booking -> booking.reservationId().equals(reservationId));
    }
}

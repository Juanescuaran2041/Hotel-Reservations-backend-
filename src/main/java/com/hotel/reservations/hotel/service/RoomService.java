package com.hotel.reservations.hotel.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.hotel.reservations.common.exception.BusinessRuleException;
import com.hotel.reservations.hotel.model.Room;
import com.hotel.reservations.hotel.model.RoomType;
import com.hotel.reservations.hotel.repository.RoomRepository;

/** Subsystem: room availability, reservation and release. */
@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    /** Lists the rooms free for the whole stay; a null type means "any type". */
    public List<Room> findAvailable(RoomType type, LocalDate from, LocalDate to) {
        List<Room> candidates = type == null ? roomRepository.findAll() : roomRepository.findByType(type);
        return candidates.stream()
                .filter(room -> room.isAvailable(from, to))
                .sorted(Comparator.comparing(Room::getNumber))
                .toList();
    }

    /** Books the first free room of the requested type. Safe under concurrent requests. */
    public Room reserve(RoomType type, LocalDate from, LocalDate to, UUID reservationId) {
        return roomRepository.findByType(type).stream()
                .filter(room -> room.book(reservationId, from, to))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "No %s room is available between %s and %s".formatted(type, from, to)));
    }

    public void release(Room room, UUID reservationId) {
        room.release(reservationId);
    }
}

package com.hotel.reservations.hotel.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotel.reservations.common.exception.BusinessRuleException;
import com.hotel.reservations.hotel.model.Room;
import com.hotel.reservations.hotel.model.RoomType;
import com.hotel.reservations.hotel.repository.RoomRepository;

/**
 * Subsystem: room availability. A room is held by the active reservation that covers the dates, so reserving
 * means saving a reservation and releasing means checking it out; there is no separate room state to keep in sync.
 */
@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    /** Lists the rooms free for the whole stay; a null type means "any type". */
    @Transactional(readOnly = true)
    public List<Room> findAvailable(RoomType type, LocalDate from, LocalDate to) {
        return type == null ? roomRepository.findAvailable(from, to) : roomRepository.findAvailableByType(type, from, to);
    }

    /** Picks the first free room of the requested type. */
    @Transactional(readOnly = true)
    public Room findFirstAvailable(RoomType type, LocalDate from, LocalDate to) {
        return roomRepository.findAvailableByType(type, from, to).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "No %s room is available between %s and %s".formatted(type, from, to)));
    }
}

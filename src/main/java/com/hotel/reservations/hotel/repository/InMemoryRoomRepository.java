package com.hotel.reservations.hotel.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.hotel.reservations.hotel.model.Room;
import com.hotel.reservations.hotel.model.RoomType;

/** Holds the hotel's predefined rooms: 6 single, 8 double and 4 suites (18 in total). */
@Repository
public class InMemoryRoomRepository implements RoomRepository {

    private final List<Room> rooms;

    public InMemoryRoomRepository() {
        List<Room> initial = new ArrayList<>();
        addFloor(initial, 1, RoomType.SINGLE, 6);
        addFloor(initial, 2, RoomType.DOUBLE, 8);
        addFloor(initial, 3, RoomType.SUITE, 4);
        this.rooms = List.copyOf(initial);
    }

    private void addFloor(List<Room> target, int floor, RoomType type, int count) {
        for (int index = 1; index <= count; index++) {
            target.add(new Room("%d%02d".formatted(floor, index), type));
        }
    }

    @Override
    public List<Room> findAll() {
        return rooms;
    }

    @Override
    public List<Room> findByType(RoomType type) {
        return rooms.stream()
                .filter(room -> room.getType() == type)
                .sorted(Comparator.comparing(Room::getNumber))
                .toList();
    }
}

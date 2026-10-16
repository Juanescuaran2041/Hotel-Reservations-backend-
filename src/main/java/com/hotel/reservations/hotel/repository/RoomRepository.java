package com.hotel.reservations.hotel.repository;

import java.util.List;

import com.hotel.reservations.hotel.model.Room;
import com.hotel.reservations.hotel.model.RoomType;

public interface RoomRepository {

    List<Room> findAll();

    List<Room> findByType(RoomType type);
}

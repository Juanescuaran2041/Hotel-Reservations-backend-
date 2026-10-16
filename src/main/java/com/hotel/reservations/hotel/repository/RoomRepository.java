package com.hotel.reservations.hotel.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hotel.reservations.hotel.model.Room;
import com.hotel.reservations.hotel.model.RoomType;

public interface RoomRepository extends JpaRepository<Room, String> {

    /**
     * Rooms of the type with no active reservation overlapping [from, to). A reservation stops holding its
     * room when it is checked out. The check-out day of one stay and the check-in day of the next may coincide.
     */
    @Query("""
            select r from Room r
            where r.type = :type
              and not exists (
                select 1 from Reservation res
                where res.room = r
                  and res.status <> com.hotel.reservations.hotel.model.ReservationStatus.CHECKED_OUT
                  and res.checkInDate < :to and :from < res.checkOutDate)
            order by r.number
            """)
    List<Room> findAvailableByType(@Param("type") RoomType type, @Param("from") LocalDate from,
                                   @Param("to") LocalDate to);

    @Query("""
            select r from Room r
            where not exists (
                select 1 from Reservation res
                where res.room = r
                  and res.status <> com.hotel.reservations.hotel.model.ReservationStatus.CHECKED_OUT
                  and res.checkInDate < :to and :from < res.checkOutDate)
            order by r.number
            """)
    List<Room> findAvailable(@Param("from") LocalDate from, @Param("to") LocalDate to);
}

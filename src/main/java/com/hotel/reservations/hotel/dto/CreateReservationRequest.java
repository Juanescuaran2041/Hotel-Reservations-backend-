package com.hotel.reservations.hotel.dto;

import java.time.LocalDate;

import com.hotel.reservations.hotel.model.RoomType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReservationRequest(
        @NotBlank @Size(max = 120) String guestName,
        @NotBlank @Email @Size(max = 160) String guestEmail,
        @Size(max = 30) String guestPhone,
        @NotNull RoomType roomType,
        @NotNull LocalDate checkInDate,
        @NotNull LocalDate checkOutDate) {
}

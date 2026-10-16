package com.hotel.reservations.hotel.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hotel.reservations.auth.security.UserPrincipal;
import com.hotel.reservations.hotel.dto.AddServiceRequest;
import com.hotel.reservations.hotel.dto.AvailabilityResponse;
import com.hotel.reservations.hotel.dto.CreateReservationRequest;
import com.hotel.reservations.hotel.dto.ReservationResponse;
import com.hotel.reservations.hotel.facade.HotelFacade;
import com.hotel.reservations.hotel.model.RoomType;

import jakarta.validation.Valid;

/**
 * REST adapter for the hotel use cases. Route names (reservar, disponibilidad, ...) follow the
 * workshop specification; everything behind them goes through {@link HotelFacade}.
 */
@RestController
@RequestMapping("/api/v1/hotel")
public class HotelController {

    private final HotelFacade hotelFacade;

    public HotelController(HotelFacade hotelFacade) {
        this.hotelFacade = hotelFacade;
    }

    @PostMapping("/reservar")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse createReservation(@Valid @RequestBody CreateReservationRequest request,
                                                 @AuthenticationPrincipal UserPrincipal user) {
        return hotelFacade.createReservation(request, user);
    }

    @GetMapping("/disponibilidad")
    public AvailabilityResponse checkAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
            @RequestParam(required = false) RoomType roomType) {
        return hotelFacade.checkAvailability(roomType, checkInDate, checkOutDate);
    }

    @PostMapping("/servicios/{reservationId}")
    public ReservationResponse addService(@PathVariable UUID reservationId,
                                          @Valid @RequestBody AddServiceRequest request,
                                          @AuthenticationPrincipal UserPrincipal user) {
        return hotelFacade.addService(reservationId, request.serviceType(), user);
    }

    @PutMapping("/checkin/{reservationId}")
    public ReservationResponse checkIn(@PathVariable UUID reservationId,
                                       @AuthenticationPrincipal UserPrincipal user) {
        return hotelFacade.checkIn(reservationId, user);
    }

    @PutMapping("/checkout/{reservationId}")
    public ReservationResponse checkOut(@PathVariable UUID reservationId,
                                        @AuthenticationPrincipal UserPrincipal user) {
        return hotelFacade.checkOut(reservationId, user);
    }

    @GetMapping("/reserva/{reservationId}")
    public ReservationResponse getReservation(@PathVariable UUID reservationId,
                                              @AuthenticationPrincipal UserPrincipal user) {
        return hotelFacade.getReservation(reservationId, user);
    }
}

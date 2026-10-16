package com.hotel.reservations.hotel.facade;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.hotel.reservations.auth.security.UserPrincipal;
import com.hotel.reservations.common.exception.AccessDeniedBusinessException;
import com.hotel.reservations.common.exception.BusinessRuleException;
import com.hotel.reservations.common.exception.InvalidRequestException;
import com.hotel.reservations.common.exception.ResourceNotFoundException;
import com.hotel.reservations.hotel.dto.AvailabilityResponse;
import com.hotel.reservations.hotel.dto.AvailableRoomResponse;
import com.hotel.reservations.hotel.dto.CreateReservationRequest;
import com.hotel.reservations.hotel.dto.ReservationResponse;
import com.hotel.reservations.hotel.model.AdditionalServiceType;
import com.hotel.reservations.hotel.model.Guest;
import com.hotel.reservations.hotel.model.Invoice;
import com.hotel.reservations.hotel.model.RateQuote;
import com.hotel.reservations.hotel.model.Reservation;
import com.hotel.reservations.hotel.model.ReservationStatus;
import com.hotel.reservations.hotel.model.Room;
import com.hotel.reservations.hotel.model.RoomType;
import com.hotel.reservations.hotel.repository.ReservationRepository;
import com.hotel.reservations.hotel.service.AccessService;
import com.hotel.reservations.hotel.service.AdditionalServiceService;
import com.hotel.reservations.hotel.service.BillingService;
import com.hotel.reservations.hotel.service.RateService;
import com.hotel.reservations.hotel.service.RoomService;

/**
 * Facade pattern: the single entry point to the hotel subsystems (rooms, rates, additional services,
 * billing and access). Controllers talk only to this class and never to the subsystems directly,
 * so each use case coordinates several services behind one simple method.
 */
@Service
public class HotelFacade {

    private static final int MAX_STAY_NIGHTS = 30;

    private final RoomService roomService;
    private final RateService rateService;
    private final AdditionalServiceService additionalServiceService;
    private final BillingService billingService;
    private final AccessService accessService;
    private final ReservationRepository reservationRepository;
    private final HotelMapper mapper;
    private final Clock clock;

    public HotelFacade(RoomService roomService, RateService rateService,
                       AdditionalServiceService additionalServiceService, BillingService billingService,
                       AccessService accessService, ReservationRepository reservationRepository,
                       HotelMapper mapper, Clock clock) {
        this.roomService = roomService;
        this.rateService = rateService;
        this.additionalServiceService = additionalServiceService;
        this.billingService = billingService;
        this.accessService = accessService;
        this.reservationRepository = reservationRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** Lists free rooms for the dates, each with its dynamic price; a null type means any type. */
    public AvailabilityResponse checkAvailability(RoomType type, LocalDate checkIn, LocalDate checkOut) {
        validateStay(checkIn, checkOut);
        List<AvailableRoomResponse> rooms = roomService.findAvailable(type, checkIn, checkOut).stream()
                .map(room -> mapper.toAvailableRoom(room, rateService.calculate(room.getType(), checkIn, checkOut)))
                .toList();
        return new AvailabilityResponse(checkIn, checkOut, ChronoUnit.DAYS.between(checkIn, checkOut), rooms);
    }

    /** Prices the stay, books a room of the requested type and stores the reservation. */
    public ReservationResponse createReservation(CreateReservationRequest request, UserPrincipal user) {
        validateStay(request.checkInDate(), request.checkOutDate());
        RateQuote quote = rateService.calculate(request.roomType(), request.checkInDate(), request.checkOutDate());
        UUID reservationId = UUID.randomUUID();
        Room room = roomService.reserve(request.roomType(), request.checkInDate(), request.checkOutDate(),
                reservationId);

        Guest guest = new Guest(request.guestName().trim(), request.guestEmail().trim(), request.guestPhone());
        Reservation reservation = new Reservation(reservationId, user.email(), guest, room,
                request.checkInDate(), request.checkOutDate(), quote);
        reservationRepository.save(reservation);
        return mapper.toResponse(reservation, null);
    }

    public ReservationResponse addService(UUID reservationId, AdditionalServiceType type, UserPrincipal user) {
        Reservation reservation = loadAuthorized(reservationId, user);
        synchronized (reservation) {
            additionalServiceService.addService(reservation, type);
            return mapper.toResponse(reservation, null);
        }
    }

    /** Marks the guest as arrived and issues the digital room key. */
    public ReservationResponse checkIn(UUID reservationId, UserPrincipal user) {
        Reservation reservation = loadAuthorized(reservationId, user);
        synchronized (reservation) {
            if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
                throw new BusinessRuleException("Check-in requires a CONFIRMED reservation but it is "
                        + reservation.getStatus());
            }
            reservation.setDigitalKey(accessService.issueKey(reservation));
            reservation.setStatus(ReservationStatus.CHECKED_IN);
            return mapper.toResponse(reservation, null);
        }
    }

    /** Closes the stay: revokes the key, frees the room and issues the final invoice. */
    public ReservationResponse checkOut(UUID reservationId, UserPrincipal user) {
        Reservation reservation = loadAuthorized(reservationId, user);
        synchronized (reservation) {
            if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
                throw new BusinessRuleException("Check-out requires a CHECKED_IN reservation but it is "
                        + reservation.getStatus());
            }
            accessService.revokeKey(reservation.getDigitalKey());
            reservation.setDigitalKey(null);
            roomService.release(reservation.getRoom(), reservation.getId());
            reservation.setStatus(ReservationStatus.CHECKED_OUT);
            Invoice invoice = billingService.generateInvoice(reservation);
            return mapper.toResponse(reservation, invoice);
        }
    }

    public ReservationResponse getReservation(UUID reservationId, UserPrincipal user) {
        Reservation reservation = loadAuthorized(reservationId, user);
        Invoice invoice = billingService.findByReservationId(reservationId).orElse(null);
        return mapper.toResponse(reservation, invoice);
    }

    private Reservation loadAuthorized(UUID reservationId, UserPrincipal user) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation " + reservationId + " not found"));
        if (!user.isAdmin() && !reservation.getOwnerEmail().equals(user.email())) {
            throw new AccessDeniedBusinessException("You cannot access this reservation");
        }
        return reservation;
    }

    private void validateStay(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new InvalidRequestException("Both checkInDate and checkOutDate are required");
        }
        if (checkIn.isBefore(LocalDate.now(clock))) {
            throw new InvalidRequestException("checkInDate cannot be in the past");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("checkOutDate must be after checkInDate");
        }
        if (ChronoUnit.DAYS.between(checkIn, checkOut) > MAX_STAY_NIGHTS) {
            throw new InvalidRequestException("A stay cannot exceed " + MAX_STAY_NIGHTS + " nights");
        }
    }
}

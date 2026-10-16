package com.hotel.reservations.hotel.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hotel.reservations.hotel.model.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    /** Highest invoice number issued so far; used to resume the numbering after a restart. */
    Optional<Invoice> findTopByOrderByNumberDesc();
}

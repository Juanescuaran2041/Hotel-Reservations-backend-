package com.hotel.reservations.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.hotel.reservations.hotel.repository.InvoiceRepository;
import com.hotel.reservations.hotel.util.InvoiceNumberGenerator;

/**
 * The invoice sequence lives in the {@link InvoiceNumberGenerator} singleton, so after a restart it would start
 * again at 1. This resumes it from the highest stored number. The unique constraint on invoices.number remains
 * the safety net; a deployment with several instances would need a database sequence instead.
 */
@Component
public class InvoiceNumberInitializer implements ApplicationRunner {

    private final InvoiceRepository invoiceRepository;

    public InvoiceNumberInitializer(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        invoiceRepository.findTopByOrderByNumberDesc().ifPresent(invoice -> {
            String number = invoice.getNumber();
            long sequence = Long.parseLong(number.substring(number.lastIndexOf('-') + 1));
            InvoiceNumberGenerator.getInstance().resumeAfter(sequence);
        });
    }
}

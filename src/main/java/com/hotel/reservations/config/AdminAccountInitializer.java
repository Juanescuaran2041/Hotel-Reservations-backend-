package com.hotel.reservations.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.hotel.reservations.auth.model.Role;
import com.hotel.reservations.auth.service.AuthService;
import com.hotel.reservations.common.exception.BusinessRuleException;

/**
 * Creates an administrator at startup, only when both ADMIN_EMAIL and ADMIN_PASSWORD are provided.
 * No default credentials exist, so a deployment without them simply has no admin.
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final AuthService authService;
    private final String adminEmail;
    private final String adminPassword;

    public AdminAccountInitializer(AuthService authService,
                                   @Value("${hotel.security.admin.email:}") String adminEmail,
                                   @Value("${hotel.security.admin.password:}") String adminPassword) {
        this.authService = authService;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            return;
        }
        try {
            authService.createUser("Administrator", adminEmail, adminPassword, Role.ADMIN);
            log.info("Administrator account created for {}", adminEmail);
        } catch (BusinessRuleException ex) {
            // The database keeps accounts across restarts, so the admin normally exists already.
            log.info("Administrator account for {} already exists", adminEmail);
        }
    }
}

package com.hotel.reservations.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings. The secret must come from the environment (never from source control) and be at
 * least 32 bytes long so it is a valid HMAC-SHA256 key.
 */
@ConfigurationProperties(prefix = "hotel.security.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "hotel.security.jwt.secret (env JWT_SECRET) must be set and have at least 32 characters");
        }
        issuer = issuer == null || issuer.isBlank() ? "hotel-reservations" : issuer;
        accessTokenTtl = accessTokenTtl == null ? Duration.ofMinutes(15) : accessTokenTtl;
        refreshTokenTtl = refreshTokenTtl == null ? Duration.ofDays(7) : refreshTokenTtl;
    }
}

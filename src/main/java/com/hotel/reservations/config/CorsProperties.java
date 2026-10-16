package com.hotel.reservations.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Origins (for example the Vercel frontend) allowed to call the API from a browser. */
@ConfigurationProperties(prefix = "hotel.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}

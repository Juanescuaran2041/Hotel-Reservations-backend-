package com.hotel.reservations.auth.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.hotel.reservations.auth.model.Role;
import com.hotel.reservations.auth.model.User;
import com.hotel.reservations.common.exception.AuthenticationFailedException;
import com.hotel.reservations.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Issues and verifies HS256-signed JWTs. Access and refresh tokens are told apart by the "type" claim. */
@Service
public class JwtService {

    private static final String TYPE_CLAIM = "type";
    private static final String ROLE_CLAIM = "role";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return generate(user, TokenType.ACCESS, properties.accessTokenTtl());
    }

    public String generateRefreshToken(User user) {
        return generate(user, TokenType.REFRESH, properties.refreshTokenTtl());
    }

    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    /** Verifies signature, issuer, expiry and token type; throws if anything is wrong. */
    public UserPrincipal parse(String token, TokenType expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expectedType.name().equals(claims.get(TYPE_CLAIM, String.class))) {
                throw new AuthenticationFailedException("Invalid token type");
            }
            return new UserPrincipal(claims.getSubject(), Role.valueOf(claims.get(ROLE_CLAIM, String.class)));
        } catch (JwtException | IllegalArgumentException ex) {
            throw new AuthenticationFailedException("Invalid or expired token");
        }
    }

    private String generate(User user, TokenType type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.issuer())
                .subject(user.getEmail())
                .claim(ROLE_CLAIM, user.getRole().name())
                .claim(TYPE_CLAIM, type.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public enum TokenType {
        ACCESS,
        REFRESH
    }
}

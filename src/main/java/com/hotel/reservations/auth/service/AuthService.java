package com.hotel.reservations.auth.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hotel.reservations.auth.dto.LoginRequest;
import com.hotel.reservations.auth.dto.RegisterRequest;
import com.hotel.reservations.auth.dto.TokenResponse;
import com.hotel.reservations.auth.dto.UserResponse;
import com.hotel.reservations.auth.model.Role;
import com.hotel.reservations.auth.model.User;
import com.hotel.reservations.auth.repository.UserRepository;
import com.hotel.reservations.auth.security.JwtService;
import com.hotel.reservations.auth.security.UserPrincipal;
import com.hotel.reservations.common.exception.AuthenticationFailedException;
import com.hotel.reservations.common.exception.BusinessRuleException;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";
    private static final String EMAIL_TAKEN = "An account with this email already exists";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    /** Compared against when the email is unknown so response time does not reveal which emails exist. */
    private final String decoyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.decoyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public UserResponse register(RegisterRequest request) {
        return UserResponse.from(createUser(request.fullName(), request.email(), request.password(), Role.GUEST));
    }

    /** Creates an account with an explicit role; used by registration and by the optional admin bootstrap. */
    public User createUser(String fullName, String email, String rawPassword, Role role) {
        String normalizedEmail = normalize(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessRuleException(EMAIL_TAKEN);
        }
        User user = new User(UUID.randomUUID(), fullName.trim(), normalizedEmail,
                passwordEncoder.encode(rawPassword), role);
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // Two concurrent registrations with the same email: the unique constraint decides.
            throw new BusinessRuleException(EMAIL_TAKEN);
        }
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.email())).orElse(null);
        boolean matches = passwordEncoder.matches(request.password(), user == null ? decoyHash : user.getPasswordHash());
        if (user == null || !matches) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        return issueTokens(user);
    }

    public TokenResponse refresh(String refreshToken) {
        UserPrincipal principal = jwtService.parse(refreshToken, JwtService.TokenType.REFRESH);
        User user = userRepository.findByEmail(principal.email())
                .orElseThrow(() -> new AuthenticationFailedException("Invalid or expired token"));
        return issueTokens(user);
    }

    public UserResponse currentUser(UserPrincipal principal) {
        return userRepository.findByEmail(principal.email())
                .map(UserResponse::from)
                .orElseThrow(() -> new AuthenticationFailedException("Account no longer exists"));
    }

    private TokenResponse issueTokens(User user) {
        return TokenResponse.bearer(jwtService.generateAccessToken(user), jwtService.generateRefreshToken(user),
                jwtService.accessTokenTtlSeconds());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

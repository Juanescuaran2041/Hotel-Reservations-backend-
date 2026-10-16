package com.hotel.reservations.auth.repository;

import java.util.Optional;

import com.hotel.reservations.auth.model.User;

public interface UserRepository {

    /** Stores the user; returns false if the email is already registered. */
    boolean saveIfAbsent(User user);

    Optional<User> findByEmail(String email);
}

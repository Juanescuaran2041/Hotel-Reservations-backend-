package com.hotel.reservations.auth.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.hotel.reservations.auth.model.User;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> usersByEmail = new ConcurrentHashMap<>();

    @Override
    public boolean saveIfAbsent(User user) {
        return usersByEmail.putIfAbsent(user.email(), user) == null;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(usersByEmail.get(email));
    }
}

package com.hotel.reservations.auth.dto;

import java.util.UUID;

import com.hotel.reservations.auth.model.Role;
import com.hotel.reservations.auth.model.User;

public record UserResponse(UUID id, String fullName, String email, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.fullName(), user.email(), user.role());
    }
}

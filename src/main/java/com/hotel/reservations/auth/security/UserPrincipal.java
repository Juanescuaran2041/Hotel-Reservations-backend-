package com.hotel.reservations.auth.security;

import com.hotel.reservations.auth.model.Role;

/** Identity extracted from a verified JWT and placed in the security context. */
public record UserPrincipal(String email, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}

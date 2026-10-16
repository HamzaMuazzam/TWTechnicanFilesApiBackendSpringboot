package com.trackingworld.tw.security;

import com.trackingworld.tw.alerts.domain.AdminRole;

/** Authenticated dashboard admin (JWT typ=ADMIN). */
public record AdminPrincipal(Long id, String username, AdminRole role) {

    public boolean isAdmin() {
        return role == AdminRole.ADMIN;
    }
}

package com.trackingworld.tw.security;

/** Authenticated app user (JWT typ=USER). */
public record AppUserPrincipal(Long userId, String deviceId) {
}

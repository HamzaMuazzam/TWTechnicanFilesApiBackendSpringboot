package com.trackingworld.tw.app.dto;

import java.time.Instant;

public record AuthResponse(String accessToken, long accessExpiresInSec, String refreshToken, UserDto user) {

    public record UserDto(Long id, String phone, String displayName, Instant createdAt) {
    }
}

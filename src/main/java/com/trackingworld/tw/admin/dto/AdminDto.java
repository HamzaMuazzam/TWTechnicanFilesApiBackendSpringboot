package com.trackingworld.tw.admin.dto;

import java.time.Instant;

public record AdminDto(Long id, String username, String role, boolean active, Instant createdAt, Instant lastLoginAt) {
}

package com.trackingworld.tw.security;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "auth.jwt")
public record JwtProperties(
        @NotNull @Size(min = 32, message = "JWT_SECRET must be at least 32 characters") String secret,
        @NotNull Duration accessTtl,
        @NotNull Duration refreshTtl
) {
}

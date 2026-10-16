package com.trackingworld.tw.common.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String timezone,
        Cors cors,
        Admin admin,
        Ingest ingest,
        Dispatch dispatch,
        RateLimit rateLimit
) {
    public record Cors(List<String> allowedOrigins) {
    }

    public record Admin(String initialPassword) {
    }

    public record Ingest(String initialApiKey) {
    }

    public record Dispatch(@Min(1) int batchSize, @Min(1000) long redispatchFixedDelayMs) {
    }

    public record RateLimit(@Min(1) int requestsPerMinute) {
    }
}

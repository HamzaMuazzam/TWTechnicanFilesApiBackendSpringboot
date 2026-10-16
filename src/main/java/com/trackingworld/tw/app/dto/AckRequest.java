package com.trackingworld.tw.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record AckRequest(@NotBlank @Size(max = 100) String deviceId,
                         @NotEmpty @Size(max = 500) List<@Valid AckItem> items) {

    public record AckItem(@NotNull Long notificationId, Instant receivedAt) {
    }
}

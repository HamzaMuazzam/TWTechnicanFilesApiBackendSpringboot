package com.trackingworld.tw.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TokenUpdateRequest(@NotBlank @Size(max = 100) String deviceId, @NotBlank @Size(max = 4096) String fcmToken) {
}

package com.trackingworld.tw.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HeartbeatRequest(
        @NotBlank @Size(max = 100) String deviceId,
        @Size(max = 50) String appVersion,
        Boolean batteryOptIgnored,
        Boolean unusedRestrictionsDisabled,
        Boolean notificationsEnabled
) {
}

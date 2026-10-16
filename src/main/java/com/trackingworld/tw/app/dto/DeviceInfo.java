package com.trackingworld.tw.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceInfo(
        @NotBlank @Size(max = 100) String deviceId,
        @Size(max = 4096) String fcmToken,
        @Size(max = 100) String manufacturer,
        @Size(max = 100) String model,
        @Size(max = 50) String osVersion,
        @Size(max = 50) String appVersion
) {
}

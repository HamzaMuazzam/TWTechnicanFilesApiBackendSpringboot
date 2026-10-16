package com.trackingworld.tw.app.dto;

import java.time.Instant;

public record DeviceDto(Long id, String deviceId, String manufacturer, String model, String osVersion,
                        String appVersion, boolean active, Instant lastSeenAt, boolean current) {
}

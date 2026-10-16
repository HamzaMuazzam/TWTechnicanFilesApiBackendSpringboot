package com.trackingworld.tw.app.dto;

import java.time.Instant;
import java.util.Map;

/** SPEC §7.4 NotificationDto. */
public record NotificationDto(
        Long id,
        Long alertId,
        String uid,
        String alertType,
        String alertTypeLabel,
        String severity,
        String title,
        String message,
        Double lat,
        Double lon,
        Instant dbdt,
        Instant sdt,
        Map<String, Object> extra,
        String status,
        boolean read,
        Instant readAt,
        Instant deliveredAt,
        Instant createdAt
) {
}

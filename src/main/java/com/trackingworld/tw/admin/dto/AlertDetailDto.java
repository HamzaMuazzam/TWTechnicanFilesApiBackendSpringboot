package com.trackingworld.tw.admin.dto;

import java.time.Instant;
import java.util.List;

/** GET /admin/alerts/{id}: alert + recipients (with deliveries) + discards. */
public record AlertDetailDto(Rows.AlertRow alert, List<Recipient> recipients, List<Discard> discards) {

    public record Recipient(Long notificationId, Long userId, String phone, String displayName, String status,
                            Instant sentAt, Instant deliveredAt, Instant readAt, Instant createdAt,
                            List<Delivery> deliveries) {
    }

    public record Delivery(Long id, Long deviceId, String deviceUuid, String manufacturer, String model,
                           boolean deviceActive, String status, int attempts, String fcmMessageId, String lastError,
                           Instant queuedAt, Instant sentAt, Instant deliveredAt, Instant nextAttemptAt) {
    }

    public record Discard(Long id, String rawPhone, String phoneE164, String reason, Instant createdAt) {
    }
}

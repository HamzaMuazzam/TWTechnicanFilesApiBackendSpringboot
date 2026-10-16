package com.trackingworld.tw.admin.dto;

import java.time.Instant;
import java.util.Map;

/** Flat grid rows returned by POST /admin/query/{entity} and the CSV export. */
public final class Rows {

    private Rows() {
    }

    public record AlertRow(Long id, Long sourceId, String sourceName, String sourceAlertId, String uid, String alertTypeCode,
                           String alertTypeLabel, String severity, String message, Double lat, Double lon, Instant dbdt,
                           Instant sdt, Map<String, Object> extra, int recipientsTotal, int recipientsAccepted,
                           int recipientsDiscarded) {
    }

    public record NotificationRow(Long id, Long alertId, String uid, String alertTypeCode, String alertMessage,
                                  Instant alertSdt, Long userId, String userPhone, String userDisplayName, String status,
                                  Instant sentAt, Instant deliveredAt, Instant readAt, Instant createdAt) {
    }

    public record DeliveryRow(Long id, Long notificationId, String notificationStatus, Long alertId, String uid,
                              Long userId, String userPhone, Long deviceId, String deviceUuid, String deviceModel,
                              String deviceManufacturer, String status, int attempts, String fcmMessageId,
                              String lastError, Instant queuedAt, Instant sentAt, Instant deliveredAt,
                              Instant nextAttemptAt) {
    }

    public record UserRow(Long id, String phoneE164, String displayName, String status, Instant createdAt,
                          Instant lastLoginAt) {
    }

    public record DeviceRow(Long id, Long userId, String userPhone, String deviceId, boolean hasFcmToken,
                            String manufacturer, String model, String osVersion, String appVersion, boolean active,
                            Instant lastSeenAt, Boolean batteryOptIgnored, Boolean unusedRestrictionsDisabled,
                            Boolean notificationsEnabled, Instant createdAt, Instant updatedAt) {
    }

    public record DiscardRow(Long id, Long alertId, String uid, String alertTypeCode, String rawPhone, String phoneE164,
                             String reason, Instant createdAt) {
    }

    public record SourceRow(Long id, String name, String apiKeyPrefix, boolean active, Instant createdAt,
                            Instant lastUsedAt) {
    }

    public record AlertTypeRow(String code, String label, String severity, String icon, String color, String channelId,
                               boolean active, int sortOrder) {
    }

    public record ConfigurationRow(Long id, String sourceId, String phoneRaw, String phoneE164, String uid, String alertType,
                                   Instant dbdt, String crtBy, Instant createdAt, Instant updatedAt) {
    }

    public record AdminRow(Long id, String username, String role, boolean active, Instant createdAt, Instant lastLoginAt) {
    }
}

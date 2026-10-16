package com.trackingworld.tw.admin.query;

import com.trackingworld.tw.alerts.domain.AdminUser;
import com.trackingworld.tw.alerts.domain.Alert;
import com.trackingworld.tw.alerts.domain.AlertType;
import com.trackingworld.tw.alerts.domain.AppUser;
import com.trackingworld.tw.alerts.domain.DiscardedRecipient;
import com.trackingworld.tw.alerts.domain.Configuration;
import com.trackingworld.tw.alerts.domain.IngestSource;
import com.trackingworld.tw.alerts.domain.Notification;
import com.trackingworld.tw.alerts.domain.NotificationDelivery;
import com.trackingworld.tw.alerts.domain.UserDevice;
import com.trackingworld.tw.alerts.repo.AdminUserRepository;
import com.trackingworld.tw.alerts.repo.AlertRepository;
import com.trackingworld.tw.alerts.repo.AlertTypeRepository;
import com.trackingworld.tw.alerts.repo.AppUserRepository;
import com.trackingworld.tw.alerts.repo.DiscardedRecipientRepository;
import com.trackingworld.tw.alerts.repo.ConfigurationRepository;
import com.trackingworld.tw.alerts.repo.IngestSourceRepository;
import com.trackingworld.tw.alerts.repo.NotificationDeliveryRepository;
import com.trackingworld.tw.alerts.repo.NotificationRepository;
import com.trackingworld.tw.alerts.repo.UserDeviceRepository;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.exceptions.BadRequestException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** SPEC §7.5 entities for /admin/query and /admin/export, with per-entity field whitelists. */
@Component
public class EntityRegistry {

    private final Map<String, EntityDef<?>> defs = new LinkedHashMap<>();

    public EntityRegistry(AlertRepository alerts, NotificationRepository notifications,
                          NotificationDeliveryRepository deliveries, AppUserRepository users,
                          UserDeviceRepository devices, DiscardedRecipientRepository discards,
                          IngestSourceRepository sources, AlertTypeRepository alertTypes, AdminUserRepository admins,
                          ConfigurationRepository configurations) {
        Sort idDesc = Sort.by(Sort.Direction.DESC, "id");

        register(new EntityDef<>("alerts", Alert.class, alerts,
                Set.of("id", "sourceAlertId", "uid", "alertTypeCode", "message", "lat", "lon", "dbdt", "sdt",
                        "recipientsTotal", "recipientsAccepted", "recipientsDiscarded",
                        "source.id", "source.name", "alertType.label", "alertType.severity"),
                Set.of("uid", "message", "sourceAlertId"), idDesc, RowMappers::alert));

        register(new EntityDef<>("notifications", Notification.class, notifications,
                Set.of("id", "status", "sentAt", "deliveredAt", "readAt", "createdAt",
                        "alert.id", "alert.uid", "alert.alertTypeCode", "alert.sdt", "alert.message",
                        "user.id", "user.phoneE164", "user.displayName"),
                Set.of("user.phoneE164", "alert.uid", "alert.message"), idDesc, RowMappers::notification));

        register(new EntityDef<>("deliveries", NotificationDelivery.class, deliveries,
                Set.of("id", "status", "attempts", "fcmMessageId", "lastError", "queuedAt", "sentAt", "deliveredAt",
                        "nextAttemptAt", "notification.id", "notification.status", "notification.user.id",
                        "notification.user.phoneE164", "notification.alert.id", "notification.alert.uid",
                        "notification.alert.alertTypeCode", "device.id", "device.deviceId", "device.model",
                        "device.manufacturer", "device.active"),
                Set.of("notification.user.phoneE164", "notification.alert.uid", "device.model", "lastError"),
                idDesc, RowMappers::delivery));

        register(new EntityDef<>("users", AppUser.class, users,
                Set.of("id", "phoneE164", "displayName", "status", "createdAt", "lastLoginAt"),
                Set.of("phoneE164", "displayName"), idDesc, RowMappers::user));

        register(new EntityDef<>("devices", UserDevice.class, devices,
                Set.of("id", "deviceId", "fcmToken", "manufacturer", "model", "osVersion", "appVersion", "active",
                        "lastSeenAt", "batteryOptIgnored", "unusedRestrictionsDisabled", "notificationsEnabled",
                        "createdAt", "updatedAt", "user.id", "user.phoneE164", "user.status"),
                Set.of("deviceId", "model", "manufacturer", "user.phoneE164"), idDesc, RowMappers::device));

        register(new EntityDef<>("discards", DiscardedRecipient.class, discards,
                Set.of("id", "rawPhone", "phoneE164", "reason", "createdAt", "alert.id", "alert.uid", "alert.alertTypeCode"),
                Set.of("rawPhone", "phoneE164", "alert.uid"), idDesc, RowMappers::discard));

        register(new EntityDef<>("configurations", Configuration.class, configurations,
                Set.of("id", "sourceId", "phoneRaw", "phoneE164", "uid", "alertType", "dbdt", "crtBy", "createdAt", "updatedAt"),
                Set.of("uid", "phoneE164", "phoneRaw", "sourceId"), idDesc, RowMappers::configuration));

        register(new EntityDef<>("sources", IngestSource.class, sources,
                Set.of("id", "name", "apiKeyPrefix", "active", "createdAt", "lastUsedAt"),
                Set.of("name", "apiKeyPrefix"), Sort.by(Sort.Direction.ASC, "id"), RowMappers::source));

        register(new EntityDef<>("alert-types", AlertType.class, alertTypes,
                Set.of("code", "label", "severity", "icon", "color", "channelId", "active", "sortOrder"),
                Set.of("code", "label"), Sort.by(Sort.Direction.ASC, "sortOrder", "code"), RowMappers::alertType));

        register(new EntityDef<>("admins", AdminUser.class, admins,
                Set.of("id", "username", "role", "active", "createdAt", "lastLoginAt"),
                Set.of("username"), Sort.by(Sort.Direction.ASC, "id"), RowMappers::admin));
    }

    private void register(EntityDef<?> def) {
        defs.put(def.name(), def);
    }

    public EntityDef<?> require(String name) {
        EntityDef<?> def = defs.get(name);
        if (def == null) {
            throw new BadRequestException(ErrorCodes.UNKNOWN_ENTITY);
        }
        return def;
    }

    public Set<String> names() {
        return defs.keySet();
    }
}

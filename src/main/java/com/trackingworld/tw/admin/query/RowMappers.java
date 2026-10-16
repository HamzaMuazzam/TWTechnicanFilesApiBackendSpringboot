package com.trackingworld.tw.admin.query;

import com.trackingworld.tw.admin.dto.Rows;
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

/** Entity -> flat row records (no secrets: hashes and FCM tokens are never exposed). */
public final class RowMappers {

    private RowMappers() {
    }

    public static Rows.AlertRow alert(Alert a) {
        AlertType t = a.getAlertType();
        return new Rows.AlertRow(a.getId(), a.getSource().getId(), a.getSource().getName(), a.getSourceAlertId(),
                a.getUid(), t.getCode(), t.getLabel(), t.getSeverity() == null ? null : t.getSeverity().name(),
                a.getMessage(), a.getLat(), a.getLon(), a.getDbdt(), a.getSdt(), a.getExtra(),
                a.getRecipientsTotal(), a.getRecipientsAccepted(), a.getRecipientsDiscarded());
    }

    public static Rows.NotificationRow notification(Notification n) {
        Alert a = n.getAlert();
        AppUser u = n.getUser();
        return new Rows.NotificationRow(n.getId(), a.getId(), a.getUid(), a.getAlertTypeCode(), a.getMessage(), a.getSdt(),
                u.getId(), u.getPhoneE164(), u.getDisplayName(), n.getStatus().name(),
                n.getSentAt(), n.getDeliveredAt(), n.getReadAt(), n.getCreatedAt());
    }

    public static Rows.DeliveryRow delivery(NotificationDelivery d) {
        Notification n = d.getNotification();
        UserDevice dev = d.getDevice();
        return new Rows.DeliveryRow(d.getId(), n.getId(), n.getStatus().name(), n.getAlert().getId(), n.getAlert().getUid(),
                n.getUser().getId(), n.getUser().getPhoneE164(), dev.getId(), dev.getDeviceId(), dev.getModel(),
                dev.getManufacturer(), d.getStatus().name(), d.getAttempts(), d.getFcmMessageId(), d.getLastError(),
                d.getQueuedAt(), d.getSentAt(), d.getDeliveredAt(), d.getNextAttemptAt());
    }

    public static Rows.UserRow user(AppUser u) {
        return new Rows.UserRow(u.getId(), u.getPhoneE164(), u.getDisplayName(), u.getStatus().name(), u.getCreatedAt(),
                u.getLastLoginAt());
    }

    public static Rows.DeviceRow device(UserDevice d) {
        return new Rows.DeviceRow(d.getId(), d.getUser().getId(), d.getUser().getPhoneE164(), d.getDeviceId(),
                d.getFcmToken() != null && !d.getFcmToken().isBlank(), d.getManufacturer(), d.getModel(),
                d.getOsVersion(), d.getAppVersion(), d.isActive(), d.getLastSeenAt(), d.getBatteryOptIgnored(),
                d.getUnusedRestrictionsDisabled(), d.getNotificationsEnabled(), d.getCreatedAt(), d.getUpdatedAt());
    }

    public static Rows.DiscardRow discard(DiscardedRecipient d) {
        return new Rows.DiscardRow(d.getId(), d.getAlert().getId(), d.getAlert().getUid(), d.getAlert().getAlertTypeCode(),
                d.getRawPhone(), d.getPhoneE164(), d.getReason().name(), d.getCreatedAt());
    }

    public static Rows.ConfigurationRow configuration(Configuration c) {
        return new Rows.ConfigurationRow(c.getId(), c.getSourceId(), c.getPhoneRaw(), c.getPhoneE164(), c.getUid(),
                c.getAlertType(), c.getDbdt(), c.getCrtBy(), c.getCreatedAt(), c.getUpdatedAt());
    }

    public static Rows.SourceRow source(IngestSource s) {
        return new Rows.SourceRow(s.getId(), s.getName(), s.getApiKeyPrefix(), s.isActive(), s.getCreatedAt(), s.getLastUsedAt());
    }

    public static Rows.AlertTypeRow alertType(AlertType t) {
        return new Rows.AlertTypeRow(t.getCode(), t.getLabel(), t.getSeverity() == null ? null : t.getSeverity().name(),
                t.getIcon(), t.getColor(), t.getChannelId(), t.isActive(), t.getSortOrder());
    }

    public static Rows.AdminRow admin(AdminUser a) {
        return new Rows.AdminRow(a.getId(), a.getUsername(), a.getRole().name(), a.isActive(), a.getCreatedAt(), a.getLastLoginAt());
    }
}

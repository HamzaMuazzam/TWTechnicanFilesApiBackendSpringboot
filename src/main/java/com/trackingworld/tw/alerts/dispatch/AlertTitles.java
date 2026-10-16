package com.trackingworld.tw.alerts.dispatch;

import com.trackingworld.tw.alerts.domain.Alert;
import com.trackingworld.tw.alerts.domain.AlertType;

import java.util.Map;

/** title = alert type label + " · " + (extra.plate if present, else uid), e.g. "Overspeed · ABC-123". */
public final class AlertTitles {

    public static final String DEFAULT_CHANNEL = "alerts_default";

    private AlertTitles() {
    }

    public static String title(Alert alert, AlertType type) {
        String label = type != null && type.getLabel() != null && !type.getLabel().isBlank() ? type.getLabel() : alert.getAlertTypeCode();
        String subject = plate(alert.getExtra());
        if (subject == null || subject.isBlank()) {
            subject = alert.getUid();
        }
        return subject == null || subject.isBlank() ? label : label + " · " + subject;
    }

    public static String channelId(AlertType type) {
        return type != null && type.getChannelId() != null && !type.getChannelId().isBlank() ? type.getChannelId() : DEFAULT_CHANNEL;
    }

    private static String plate(Map<String, Object> extra) {
        if (extra == null) {
            return null;
        }
        Object plate = extra.get("plate");
        return plate == null ? null : String.valueOf(plate);
    }
}

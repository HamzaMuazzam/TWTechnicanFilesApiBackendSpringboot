package com.trackingworld.tw.alerts.dispatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackingworld.tw.alerts.domain.Alert;
import com.trackingworld.tw.alerts.domain.AlertType;
import com.trackingworld.tw.alerts.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** SPEC §6.1 - FCM data payload (all values strings). */
@Slf4j
@Component
@RequiredArgsConstructor
public class FcmPayloadBuilder {

    private final ObjectMapper objectMapper;

    public Map<String, String> build(Notification notification) {
        Alert alert = notification.getAlert();
        AlertType type = alert.getAlertType();
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", "ALERT");
        data.put("notificationId", String.valueOf(notification.getId()));
        data.put("alertId", String.valueOf(alert.getId()));
        data.put("uid", nz(alert.getUid()));
        data.put("alertType", nz(type.getCode()));
        data.put("alertTypeLabel", nz(type.getLabel()));
        data.put("severity", type.getSeverity() == null ? "INFO" : type.getSeverity().name());
        data.put("title", AlertTitles.title(alert, type));
        data.put("message", nz(alert.getMessage()));
        data.put("lat", alert.getLat() == null ? "" : String.valueOf(alert.getLat()));
        data.put("lon", alert.getLon() == null ? "" : String.valueOf(alert.getLon()));
        data.put("dbdt", alert.getDbdt() == null ? "" : alert.getDbdt().toString());
        data.put("sdt", alert.getSdt() == null ? "" : alert.getSdt().toString());
        data.put("extra", extraJson(alert.getExtra()));
        data.put("channelId", AlertTitles.channelId(type));
        return data;
    }

    private String extraJson(Map<String, Object> extra) {
        if (extra == null || extra.isEmpty()) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(extra);
        } catch (JsonProcessingException e) {
            log.warn("[DISPATCH] cannot serialise extra: {}", e.getMessage());
            return "";
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}

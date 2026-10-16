package com.trackingworld.tw.app;

import com.trackingworld.tw.alerts.dispatch.AlertTitles;
import com.trackingworld.tw.alerts.domain.Alert;
import com.trackingworld.tw.alerts.domain.AlertType;
import com.trackingworld.tw.alerts.domain.Notification;
import com.trackingworld.tw.app.dto.NotificationDto;
import org.springframework.stereotype.Component;

@Component
public class NotificationDtoMapper {

    public NotificationDto toDto(Notification n) {
        Alert a = n.getAlert();
        AlertType t = a.getAlertType();
        return new NotificationDto(
                n.getId(),
                a.getId(),
                a.getUid(),
                t.getCode(),
                t.getLabel(),
                t.getSeverity() == null ? null : t.getSeverity().name(),
                AlertTitles.title(a, t),
                a.getMessage(),
                a.getLat(),
                a.getLon(),
                a.getDbdt(),
                a.getSdt(),
                a.getExtra(),
                n.getStatus().name(),
                n.isRead(),
                n.getReadAt(),
                n.getDeliveredAt(),
                n.getCreatedAt()
        );
    }
}

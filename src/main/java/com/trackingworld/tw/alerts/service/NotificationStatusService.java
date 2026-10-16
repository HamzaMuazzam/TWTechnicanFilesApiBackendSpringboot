package com.trackingworld.tw.alerts.service;

import com.trackingworld.tw.alerts.domain.DeliveryStatus;
import com.trackingworld.tw.alerts.domain.Notification;
import com.trackingworld.tw.alerts.domain.NotificationDelivery;
import com.trackingworld.tw.alerts.domain.NotificationStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;

/**
 * Derives {@code notification.status} from its deliveries (SPEC §5):
 * PENDING at creation; NO_DEVICE if there are no deliveries; SENT when the first delivery is SENT;
 * DELIVERED when the first delivery is acked; READ when the user marks it read (READ wins over everything);
 * FAILED when all deliveries are FAILED / INVALID_TOKEN.
 */
@Service
public class NotificationStatusService {

    public void recompute(Notification notification, Collection<NotificationDelivery> deliveries) {
        if (notification.getReadAt() != null) {
            notification.setStatus(NotificationStatus.READ);
            return;
        }
        if (deliveries == null || deliveries.isEmpty()) {
            notification.setStatus(NotificationStatus.NO_DEVICE);
            return;
        }
        boolean anyDelivered = false;
        boolean anySent = false;
        boolean allDead = true;
        Instant firstSent = null;
        Instant firstDelivered = null;
        for (NotificationDelivery d : deliveries) {
            switch (d.getStatus()) {
                case DELIVERED -> {
                    anyDelivered = true;
                    allDead = false;
                    firstDelivered = min(firstDelivered, d.getDeliveredAt());
                    firstSent = min(firstSent, d.getSentAt());
                }
                case SENT -> {
                    anySent = true;
                    allDead = false;
                    firstSent = min(firstSent, d.getSentAt());
                }
                case QUEUED -> allDead = false;
                case FAILED, INVALID_TOKEN -> {
                    // dead delivery
                }
            }
        }
        if (anyDelivered) {
            notification.setStatus(NotificationStatus.DELIVERED);
            if (notification.getDeliveredAt() == null) {
                notification.setDeliveredAt(firstDelivered != null ? firstDelivered : Instant.now());
            }
            if (notification.getSentAt() == null) {
                notification.setSentAt(firstSent != null ? firstSent : notification.getDeliveredAt());
            }
        } else if (anySent) {
            notification.setStatus(NotificationStatus.SENT);
            if (notification.getSentAt() == null) {
                notification.setSentAt(firstSent != null ? firstSent : Instant.now());
            }
        } else if (allDead) {
            notification.setStatus(NotificationStatus.FAILED);
        } else {
            notification.setStatus(NotificationStatus.PENDING);
        }
    }

    public boolean isDead(DeliveryStatus status) {
        return status == DeliveryStatus.FAILED || status == DeliveryStatus.INVALID_TOKEN;
    }

    private static Instant min(Instant a, Instant b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isBefore(b) ? a : b;
    }
}

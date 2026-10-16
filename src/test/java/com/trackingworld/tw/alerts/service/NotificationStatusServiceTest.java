package com.trackingworld.tw.alerts.service;

import com.trackingworld.tw.alerts.domain.DeliveryStatus;
import com.trackingworld.tw.alerts.domain.Notification;
import com.trackingworld.tw.alerts.domain.NotificationDelivery;
import com.trackingworld.tw.alerts.domain.NotificationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationStatusServiceTest {

    private final NotificationStatusService service = new NotificationStatusService();

    @Test
    void noDeliveriesMeansNoDevice() {
        Notification n = Notification.builder().build();
        service.recompute(n, List.of());
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.NO_DEVICE);
    }

    @Test
    void firstSentDeliveryMarksSent() {
        Notification n = Notification.builder().build();
        service.recompute(n, List.of(delivery(DeliveryStatus.SENT), delivery(DeliveryStatus.QUEUED)));
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(n.getSentAt()).isNotNull();
    }

    @Test
    void deliveredWinsOverSent() {
        Notification n = Notification.builder().build();
        service.recompute(n, List.of(delivery(DeliveryStatus.SENT), delivery(DeliveryStatus.DELIVERED)));
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
        assertThat(n.getDeliveredAt()).isNotNull();
    }

    @Test
    void allDeadMeansFailed() {
        Notification n = Notification.builder().build();
        service.recompute(n, List.of(delivery(DeliveryStatus.FAILED), delivery(DeliveryStatus.INVALID_TOKEN)));
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void queuedWithSomeDeadStaysPending() {
        Notification n = Notification.builder().build();
        service.recompute(n, List.of(delivery(DeliveryStatus.FAILED), delivery(DeliveryStatus.QUEUED)));
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    void readWinsOverEverything() {
        Notification n = Notification.builder().readAt(Instant.now()).build();
        service.recompute(n, List.of(delivery(DeliveryStatus.DELIVERED)));
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.READ);
    }

    private static NotificationDelivery delivery(DeliveryStatus status) {
        Instant now = Instant.now();
        return NotificationDelivery.builder()
                .status(status)
                .sentAt(status == DeliveryStatus.SENT || status == DeliveryStatus.DELIVERED ? now : null)
                .deliveredAt(status == DeliveryStatus.DELIVERED ? now : null)
                .build();
    }
}

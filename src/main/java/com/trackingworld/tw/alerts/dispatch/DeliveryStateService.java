package com.trackingworld.tw.alerts.dispatch;

import com.trackingworld.tw.alerts.domain.DeliveryStatus;
import com.trackingworld.tw.alerts.domain.Notification;
import com.trackingworld.tw.alerts.domain.NotificationDelivery;
import com.trackingworld.tw.alerts.domain.UserDevice;
import com.trackingworld.tw.alerts.repo.NotificationDeliveryRepository;
import com.trackingworld.tw.alerts.repo.NotificationRepository;
import com.trackingworld.tw.alerts.repo.UserDeviceRepository;
import com.trackingworld.tw.alerts.service.AppSettingService;
import com.trackingworld.tw.alerts.service.NotificationStatusService;
import com.trackingworld.tw.firebase.FcmSendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Transactional state changes for deliveries + derived notification status (kept out of the async orchestrator). */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryStateService {

    static final Duration BASE_BACKOFF = Duration.ofSeconds(30);

    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationRepository notificationRepository;
    private final UserDeviceRepository deviceRepository;
    private final NotificationStatusService statusService;
    private final AppSettingService settings;

    @Transactional(readOnly = true)
    public List<NotificationDelivery> loadForDispatch(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return deliveryRepository.findForDispatch(ids);
    }

    @Transactional
    public void applyResults(List<FcmSendResult> results) {
        if (results.isEmpty()) {
            return;
        }
        int maxAttempts = settings.dispatchMaxAttempts();
        Instant now = Instant.now();
        Set<Long> touchedNotifications = new LinkedHashSet<>();
        List<Long> deadDeviceIds = new ArrayList<>();

        for (FcmSendResult result : results) {
            NotificationDelivery delivery = deliveryRepository.findById(result.deliveryId()).orElse(null);
            if (delivery == null) {
                continue;
            }
            delivery.setAttempts(delivery.getAttempts() + 1);
            if (result.success()) {
                delivery.setStatus(DeliveryStatus.SENT);
                delivery.setSentAt(now);
                delivery.setFcmMessageId(result.messageId());
                delivery.setLastError(null);
                delivery.setNextAttemptAt(null);
            } else if (result.failure() == FcmSendResult.Failure.INVALID_TOKEN) {
                delivery.setStatus(DeliveryStatus.INVALID_TOKEN);
                delivery.setLastError(truncate(result.error()));
                delivery.setNextAttemptAt(null);
                deadDeviceIds.add(delivery.getDevice().getId());
            } else {
                delivery.setStatus(DeliveryStatus.FAILED);
                delivery.setLastError(truncate(result.error()));
                if (delivery.getAttempts() >= maxAttempts) {
                    delivery.setNextAttemptAt(null);
                    log.warn("[DISPATCH] delivery {} gave up after {} attempts: {}", delivery.getId(), delivery.getAttempts(), result.error());
                } else {
                    long factor = 1L << Math.min(delivery.getAttempts(), 20);
                    delivery.setNextAttemptAt(now.plus(BASE_BACKOFF.multipliedBy(factor)));
                }
            }
            touchedNotifications.add(delivery.getNotification().getId());
        }

        for (Long deviceId : deadDeviceIds) {
            deviceRepository.findById(deviceId).ifPresent(device -> {
                device.setActive(false);
                device.setFcmToken(null);
                log.info("[DISPATCH] device {} ({}) token invalid -> deactivated", device.getId(), device.getDeviceId());
            });
        }
        for (Long notificationId : touchedNotifications) {
            recomputeNotification(notificationId);
        }
    }

    @Transactional
    public void recomputeNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) {
            return;
        }
        List<NotificationDelivery> deliveries = deliveryRepository.findByNotificationIdOrderByIdAsc(notificationId);
        statusService.recompute(notification, deliveries);
    }

    /** Requeue every delivery of a notification for the user's current active devices (admin resend). */
    @Transactional
    public int requeue(Notification notification) {
        Instant now = Instant.now();
        List<NotificationDelivery> existing = deliveryRepository.findByNotificationIdOrderByIdAsc(notification.getId());
        List<UserDevice> devices = deviceRepository.findActiveWithToken(notification.getUser().getId());
        int requeued = 0;
        for (UserDevice device : devices) {
            NotificationDelivery delivery = existing.stream()
                    .filter(d -> d.getDevice().getId().equals(device.getId()))
                    .findFirst()
                    .orElse(null);
            if (delivery == null) {
                delivery = NotificationDelivery.builder()
                        .notification(notification)
                        .device(device)
                        .build();
            }
            delivery.setStatus(DeliveryStatus.QUEUED);
            delivery.setAttempts(0);
            delivery.setLastError(null);
            delivery.setFcmMessageId(null);
            delivery.setSentAt(null);
            delivery.setDeliveredAt(null);
            delivery.setQueuedAt(now);
            delivery.setNextAttemptAt(now);
            deliveryRepository.save(delivery);
            requeued++;
        }
        List<NotificationDelivery> all = deliveryRepository.findByNotificationIdOrderByIdAsc(notification.getId());
        statusService.recompute(notification, all);
        return requeued;
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= 1000 ? s : s.substring(0, 1000);
    }
}

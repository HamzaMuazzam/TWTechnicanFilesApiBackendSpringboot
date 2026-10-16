package com.trackingworld.tw.admin;

import com.trackingworld.tw.admin.dto.AlertDetailDto;
import com.trackingworld.tw.admin.dto.ResendResultDto;
import com.trackingworld.tw.admin.query.RowMappers;
import com.trackingworld.tw.alerts.dispatch.DeliveryStateService;
import com.trackingworld.tw.alerts.dispatch.DispatchService;
import com.trackingworld.tw.alerts.domain.Alert;
import com.trackingworld.tw.alerts.domain.DiscardedRecipient;
import com.trackingworld.tw.alerts.domain.Notification;
import com.trackingworld.tw.alerts.domain.NotificationDelivery;
import com.trackingworld.tw.alerts.domain.UserDevice;
import com.trackingworld.tw.alerts.repo.AlertRepository;
import com.trackingworld.tw.alerts.repo.DiscardedRecipientRepository;
import com.trackingworld.tw.alerts.repo.NotificationDeliveryRepository;
import com.trackingworld.tw.alerts.repo.NotificationRepository;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAlertService {

    private final AlertRepository alertRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final DiscardedRecipientRepository discardedRepository;
    private final DeliveryStateService deliveryStateService;
    private final DispatchService dispatchService;

    @Transactional(readOnly = true)
    public AlertDetailDto detail(Long alertId) {
        Alert alert = alertRepository.findById(alertId).orElseThrow(() -> new NotFoundException(ErrorCodes.NOT_FOUND));
        List<Notification> notifications = notificationRepository.findByAlertIdOrderByIdAsc(alertId);
        Map<Long, List<NotificationDelivery>> deliveriesByNotification = notifications.isEmpty()
                ? Map.of()
                : deliveryRepository.findByNotificationIdIn(notifications.stream().map(Notification::getId).toList())
                .stream().collect(Collectors.groupingBy(d -> d.getNotification().getId()));
        List<AlertDetailDto.Recipient> recipients = notifications.stream().map(n -> new AlertDetailDto.Recipient(
                n.getId(), n.getUser().getId(), n.getUser().getPhoneE164(), n.getUser().getDisplayName(),
                n.getStatus().name(), n.getSentAt(), n.getDeliveredAt(), n.getReadAt(), n.getCreatedAt(),
                deliveriesByNotification.getOrDefault(n.getId(), List.of()).stream()
                        .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                        .map(AdminAlertService::toDelivery).toList()
        )).toList();
        List<AlertDetailDto.Discard> discards = discardedRepository.findByAlertIdOrderByIdAsc(alertId).stream()
                .map(AdminAlertService::toDiscard).toList();
        return new AlertDetailDto(RowMappers.alert(alert), recipients, discards);
    }

    /** POST /admin/notifications/{id}/resend - requeue deliveries for the user's active devices and dispatch. */
    @Transactional
    public ResendResultDto resend(Long notificationId, String adminUsername) {
        Notification notification = notificationRepository.findWithDetailsById(notificationId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.NOT_FOUND));
        int requeued = deliveryStateService.requeue(notification);
        log.info("[ADMIN] resend notification={} by={} requeued={}", notificationId, adminUsername, requeued);
        if (requeued > 0) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatchService.dispatchNotificationAsync(notificationId);
                }
            });
        }
        return new ResendResultDto(notificationId, requeued);
    }

    private static AlertDetailDto.Delivery toDelivery(NotificationDelivery d) {
        UserDevice dev = d.getDevice();
        return new AlertDetailDto.Delivery(d.getId(), dev.getId(), dev.getDeviceId(), dev.getManufacturer(), dev.getModel(),
                dev.isActive(), d.getStatus().name(), d.getAttempts(), d.getFcmMessageId(), d.getLastError(),
                d.getQueuedAt(), d.getSentAt(), d.getDeliveredAt(), d.getNextAttemptAt());
    }

    private static AlertDetailDto.Discard toDiscard(DiscardedRecipient d) {
        return new AlertDetailDto.Discard(d.getId(), d.getRawPhone(), d.getPhoneE164(), d.getReason().name(), d.getCreatedAt());
    }
}

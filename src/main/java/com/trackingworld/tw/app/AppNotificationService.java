package com.trackingworld.tw.app;

import com.trackingworld.tw.alerts.dispatch.DeliveryStateService;
import com.trackingworld.tw.alerts.domain.AlertType;
import com.trackingworld.tw.alerts.domain.DeliveryStatus;
import com.trackingworld.tw.alerts.domain.Notification;
import com.trackingworld.tw.alerts.domain.NotificationDelivery;
import com.trackingworld.tw.alerts.domain.NotificationStatus;
import com.trackingworld.tw.alerts.domain.UserDevice;
import com.trackingworld.tw.alerts.repo.AlertTypeRepository;
import com.trackingworld.tw.alerts.repo.NotificationDeliveryRepository;
import com.trackingworld.tw.alerts.repo.NotificationRepository;
import com.trackingworld.tw.alerts.repo.UserDeviceRepository;
import com.trackingworld.tw.app.dto.AckRequest;
import com.trackingworld.tw.app.dto.AlertTypeDto;
import com.trackingworld.tw.app.dto.NotificationDto;
import com.trackingworld.tw.app.dto.NotificationPageDto;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.exceptions.NotFoundException;
import com.trackingworld.tw.alerts.domain.Alert;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** SPEC §7.4 app notifications: keyset inbox, detail, unread count, ack, read, read-all, alert types. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppNotificationService {

    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_SIZE = 30;

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final UserDeviceRepository deviceRepository;
    private final AlertTypeRepository alertTypeRepository;
    private final DeliveryStateService deliveryStateService;
    private final NotificationDtoMapper mapper;

    @Transactional(readOnly = true)
    public NotificationPageDto list(Long userId, Long cursor, Integer size, boolean unreadOnly, String alertType) {
        int pageSize = size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        Specification<Notification> spec = (root, query, cb) -> {
            @SuppressWarnings("unchecked")
            Join<Notification, Alert> alert = (Join<Notification, Alert>) root.<Notification, Alert>fetch("alert", JoinType.INNER);
            alert.fetch("alertType", JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            if (cursor != null) {
                predicates.add(cb.lessThan(root.get("id"), cursor));
            }
            if (unreadOnly) {
                predicates.add(cb.isNull(root.get("readAt")));
            }
            if (alertType != null && !alertType.isBlank()) {
                predicates.add(cb.equal(alert.get("alertTypeCode"), alertType.trim()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        List<Notification> rows = notificationRepository.findBy(spec, q -> q
                .sortBy(Sort.by(Sort.Direction.DESC, "id"))
                .limit(pageSize + 1)
                .all());
        boolean hasMore = rows.size() > pageSize;
        List<Notification> page = hasMore ? rows.subList(0, pageSize) : rows;
        List<NotificationDto> items = page.stream().map(mapper::toDto).toList();
        Long nextCursor = hasMore ? page.get(page.size() - 1).getId() : null;
        return new NotificationPageDto(items, nextCursor);
    }

    @Transactional(readOnly = true)
    public NotificationDto get(Long userId, Long id) {
        return notificationRepository.findByIdAndUserId(id, userId)
                .map(mapper::toDto)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    /** Device received the push -> delivery DELIVERED, notification DELIVERED (unless READ). Idempotent. */
    @Transactional
    public int ack(Long userId, AckRequest request) {
        UserDevice device = deviceRepository.findByUserIdAndDeviceId(userId, request.deviceId())
                .orElseThrow(() -> new NotFoundException(ErrorCodes.DEVICE_NOT_FOUND));
        Instant now = Instant.now();
        device.setLastSeenAt(now);
        int updated = 0;
        for (AckRequest.AckItem item : request.items()) {
            Notification notification = notificationRepository.findByIdAndUserId(item.notificationId(), userId).orElse(null);
            if (notification == null) {
                continue;
            }
            Instant receivedAt = item.receivedAt() != null && !item.receivedAt().isAfter(now) ? item.receivedAt() : now;
            NotificationDelivery delivery = deliveryRepository
                    .findByNotificationIdAndDeviceId(notification.getId(), device.getId())
                    .orElseGet(() -> deliveryRepository.save(NotificationDelivery.builder()
                            .notification(notification)
                            .device(device)
                            .status(DeliveryStatus.SENT)
                            .queuedAt(receivedAt)
                            .sentAt(receivedAt)
                            .build()));
            if (delivery.getStatus() != DeliveryStatus.DELIVERED) {
                delivery.setStatus(DeliveryStatus.DELIVERED);
                delivery.setDeliveredAt(receivedAt);
                if (delivery.getSentAt() == null) {
                    delivery.setSentAt(receivedAt);
                }
                delivery.setNextAttemptAt(null);
                delivery.setLastError(null);
                updated++;
            }
            if (notification.getStatus() != NotificationStatus.READ) {
                deliveryStateService.recomputeNotification(notification.getId());
            }
        }
        log.info("[APP] ack user={} device={} items={} updated={}", userId, request.deviceId(), request.items().size(), updated);
        return updated;
    }

    @Transactional
    public int markRead(Long userId, List<Long> ids) {
        int updated = notificationRepository.markRead(userId, ids, Instant.now(), NotificationStatus.READ);
        log.info("[APP] read user={} ids={} updated={}", userId, ids.size(), updated);
        return updated;
    }

    @Transactional
    public int markAllRead(Long userId, Long beforeId) {
        Instant now = Instant.now();
        int updated = beforeId == null
                ? notificationRepository.markAllRead(userId, now, NotificationStatus.READ)
                : notificationRepository.markAllReadBefore(userId, beforeId, now, NotificationStatus.READ);
        log.info("[APP] read-all user={} beforeId={} updated={}", userId, beforeId, updated);
        return updated;
    }

    @Transactional(readOnly = true)
    public List<AlertTypeDto> alertTypes() {
        return alertTypeRepository.findByActiveTrueOrderBySortOrderAscCodeAsc().stream()
                .map(AppNotificationService::toDto)
                .toList();
    }

    static AlertTypeDto toDto(AlertType t) {
        return new AlertTypeDto(t.getCode(), t.getLabel(), t.getSeverity() == null ? null : t.getSeverity().name(),
                t.getIcon(), t.getColor());
    }
}

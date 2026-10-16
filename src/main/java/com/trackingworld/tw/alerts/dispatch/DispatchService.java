package com.trackingworld.tw.alerts.dispatch;

import com.trackingworld.tw.alerts.domain.DeliveryStatus;
import com.trackingworld.tw.alerts.domain.NotificationDelivery;
import com.trackingworld.tw.alerts.domain.UserDevice;
import com.trackingworld.tw.alerts.repo.NotificationDeliveryRepository;
import com.trackingworld.tw.common.config.AppProperties;
import com.trackingworld.tw.common.config.AsyncConfig;
import com.trackingworld.tw.firebase.FcmMessage;
import com.trackingworld.tw.firebase.FcmSendResult;
import com.trackingworld.tw.firebase.FcmSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SPEC §6 delivery pipeline: after an alert commits, QUEUED deliveries are pushed via FCM asynchronously;
 * a 30s scheduler re-dispatches QUEUED/FAILED deliveries whose next_attempt_at has passed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchService {

    private static final List<DeliveryStatus> DUE_STATUSES = List.of(DeliveryStatus.QUEUED, DeliveryStatus.FAILED);

    private final NotificationDeliveryRepository deliveryRepository;
    private final DeliveryStateService stateService;
    private final FcmPayloadBuilder payloadBuilder;
    private final FcmSender fcmSender;
    private final AppProperties appProperties;

    /** Delivery ids currently being sent (single-instance guard against the scheduler racing the async path). */
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();

    @Async(AsyncConfig.DISPATCH_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAlertAccepted(AlertAcceptedEvent event) {
        dispatchAlert(event.alertId());
    }

    public void dispatchAlert(Long alertId) {
        List<Long> ids = deliveryRepository.findIdsByAlertAndStatus(alertId, DeliveryStatus.QUEUED);
        log.info("[DISPATCH] alert={} queuedDeliveries={}", alertId, ids.size());
        process(ids);
    }

    @Async(AsyncConfig.DISPATCH_EXECUTOR)
    public void dispatchNotificationAsync(Long notificationId) {
        List<Long> ids = deliveryRepository.findIdsByNotificationAndStatus(notificationId, DeliveryStatus.QUEUED);
        log.info("[DISPATCH] notification={} queuedDeliveries={}", notificationId, ids.size());
        process(ids);
    }

    @Scheduled(fixedDelayString = "${app.dispatch.redispatch-fixed-delay-ms:30000}", initialDelay = 15_000)
    public void redispatchDue() {
        int batch = appProperties.dispatch().batchSize();
        List<Long> ids = deliveryRepository.findDueIds(DUE_STATUSES, Instant.now(), PageRequest.of(0, batch));
        if (ids.isEmpty()) {
            return;
        }
        log.info("[DISPATCH][SCHED] due deliveries={}", ids.size());
        process(ids);
    }

    public int inFlightCount() {
        return inFlight.size();
    }

    /** Sends the given deliveries (skipping any already in flight) and persists the outcome. */
    public void process(List<Long> deliveryIds) {
        List<Long> claimed = new ArrayList<>();
        for (Long id : deliveryIds) {
            if (inFlight.add(id)) {
                claimed.add(id);
            }
        }
        if (claimed.isEmpty()) {
            return;
        }
        long start = System.currentTimeMillis();
        try {
            List<NotificationDelivery> deliveries = stateService.loadForDispatch(claimed);
            List<FcmMessage> messages = new ArrayList<>();
            List<FcmSendResult> results = new ArrayList<>();
            for (NotificationDelivery delivery : deliveries) {
                if (delivery.getStatus() != DeliveryStatus.QUEUED && delivery.getStatus() != DeliveryStatus.FAILED) {
                    continue;
                }
                UserDevice device = delivery.getDevice();
                if (!device.isActive() || device.getFcmToken() == null || device.getFcmToken().isBlank()) {
                    results.add(FcmSendResult.invalidToken(delivery.getId(), "device inactive or without FCM token"));
                    continue;
                }
                messages.add(new FcmMessage(delivery.getId(), device.getFcmToken(), payloadBuilder.build(delivery.getNotification())));
            }
            if (!messages.isEmpty()) {
                results.addAll(fcmSender.sendEach(messages));
            }
            stateService.applyResults(results);
            long sent = results.stream().filter(FcmSendResult::success).count();
            long invalid = results.stream().filter(r -> r.failure() == FcmSendResult.Failure.INVALID_TOKEN).count();
            long failed = results.size() - sent - invalid;
            log.info("[DISPATCH] processed={} sent={} invalidToken={} failed={} in {} ms",
                    results.size(), sent, invalid, failed, System.currentTimeMillis() - start);
        } catch (RuntimeException e) {
            log.error("[DISPATCH] batch of {} deliveries failed: {}", claimed.size(), e.getMessage(), e);
        } finally {
            claimed.forEach(inFlight::remove);
        }
    }
}

package com.trackingworld.tw.admin;

import com.trackingworld.tw.admin.dto.DispatchStatusDto;
import com.trackingworld.tw.alerts.dispatch.DispatchService;
import com.trackingworld.tw.alerts.domain.DeliveryStatus;
import com.trackingworld.tw.alerts.domain.NotificationStatus;
import com.trackingworld.tw.alerts.repo.NotificationDeliveryRepository;
import com.trackingworld.tw.alerts.repo.NotificationRepository;
import com.trackingworld.tw.common.ApiPaths;
import com.trackingworld.tw.common.ResponseWrapper;
import com.trackingworld.tw.firebase.FirebaseProperties;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admin - Dispatch")
public class AdminDispatchController {

    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationRepository notificationRepository;
    private final DispatchService dispatchService;
    private final FirebaseProperties firebaseProperties;

    @GetMapping(ApiPaths.ADMIN + "/dispatch/status")
    @Transactional(readOnly = true)
    public ResponseWrapper<DispatchStatusDto> status() {
        Map<String, Long> deliveries = new LinkedHashMap<>();
        for (DeliveryStatus s : DeliveryStatus.values()) {
            deliveries.put(s.name(), deliveryRepository.countByStatus(s));
        }
        Map<String, Long> notifications = new LinkedHashMap<>();
        for (NotificationStatus s : NotificationStatus.values()) {
            notifications.put(s.name(), notificationRepository.countByStatus(s));
        }
        long due = deliveryRepository.countDue(List.of(DeliveryStatus.QUEUED, DeliveryStatus.FAILED), Instant.now());
        return ResponseWrapper.success(new DispatchStatusDto(deliveries, due, dispatchService.inFlightCount(),
                notifications, firebaseProperties.enabled()));
    }
}

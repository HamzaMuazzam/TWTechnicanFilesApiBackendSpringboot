package com.trackingworld.tw.app;

import com.trackingworld.tw.alerts.domain.UserDevice;
import com.trackingworld.tw.alerts.repo.UserDeviceRepository;
import com.trackingworld.tw.app.dto.DeviceDto;
import com.trackingworld.tw.app.dto.HeartbeatRequest;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.exceptions.NotFoundException;
import com.trackingworld.tw.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** SPEC §7.3 app devices. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppDeviceService {

    private final UserDeviceRepository deviceRepository;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public void updateToken(Long userId, String deviceId, String fcmToken) {
        UserDevice device = require(userId, deviceId);
        Instant now = Instant.now();
        device.setFcmToken(fcmToken.trim());
        device.setActive(true);
        device.setLastSeenAt(now);
        device.setUpdatedAt(now);
        deviceRepository.clearTokenElsewhere(device.getFcmToken(), device.getId(), now);
        log.info("[DEVICE] token updated user={} device={}", userId, deviceId);
    }

    @Transactional
    public void heartbeat(Long userId, HeartbeatRequest request) {
        UserDevice device = require(userId, request.deviceId());
        Instant now = Instant.now();
        if (request.appVersion() != null && !request.appVersion().isBlank()) {
            device.setAppVersion(request.appVersion());
        }
        if (request.batteryOptIgnored() != null) {
            device.setBatteryOptIgnored(request.batteryOptIgnored());
        }
        if (request.unusedRestrictionsDisabled() != null) {
            device.setUnusedRestrictionsDisabled(request.unusedRestrictionsDisabled());
        }
        if (request.notificationsEnabled() != null) {
            device.setNotificationsEnabled(request.notificationsEnabled());
        }
        device.setActive(true);
        device.setLastSeenAt(now);
        device.setUpdatedAt(now);
        log.debug("[DEVICE] heartbeat user={} device={} app={}", userId, request.deviceId(), request.appVersion());
    }

    @Transactional(readOnly = true)
    public List<DeviceDto> list(Long userId, String currentDeviceId) {
        return deviceRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(d -> toDto(d, d.getDeviceId().equals(currentDeviceId)))
                .toList();
    }

    @Transactional
    public void delete(Long userId, String deviceId) {
        UserDevice device = require(userId, deviceId);
        refreshTokenService.revokeDevice(userId, deviceId);
        deviceRepository.delete(device);
        log.info("[DEVICE] deleted user={} device={}", userId, deviceId);
    }

    private UserDevice require(Long userId, String deviceId) {
        return deviceRepository.findByUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.DEVICE_NOT_FOUND));
    }

    static DeviceDto toDto(UserDevice d, boolean current) {
        return new DeviceDto(d.getId(), d.getDeviceId(), d.getManufacturer(), d.getModel(), d.getOsVersion(),
                d.getAppVersion(), d.isActive(), d.getLastSeenAt(), current);
    }
}

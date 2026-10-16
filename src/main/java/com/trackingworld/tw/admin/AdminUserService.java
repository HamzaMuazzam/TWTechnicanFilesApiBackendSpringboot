package com.trackingworld.tw.admin;

import com.trackingworld.tw.admin.dto.Rows;
import com.trackingworld.tw.admin.dto.UserDetailDto;
import com.trackingworld.tw.admin.dto.UserCreateRequest;
import com.trackingworld.tw.admin.dto.UserPatchRequest;
import com.trackingworld.tw.admin.query.RowMappers;
import com.trackingworld.tw.alerts.domain.AppUser;
import com.trackingworld.tw.alerts.domain.SubjectType;
import com.trackingworld.tw.alerts.domain.UserDevice;
import com.trackingworld.tw.alerts.domain.UserStatus;
import com.trackingworld.tw.alerts.repo.AppUserRepository;
import com.trackingworld.tw.alerts.repo.NotificationRepository;
import com.trackingworld.tw.alerts.repo.UserDeviceRepository;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.exceptions.BadRequestException;
import com.trackingworld.tw.common.exceptions.NotFoundException;
import com.trackingworld.tw.common.util.PhoneNormalizer;
import com.trackingworld.tw.security.RefreshTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AppUserRepository userRepository;
    private final UserDeviceRepository deviceRepository;
    private final NotificationRepository notificationRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    /** Create an app user with a BCrypt-hashed password; if the phone exists, (re)set its password instead. */
    @Transactional
    public Rows.UserRow create(UserCreateRequest request, String admin) {
        String phone = PhoneNormalizer.normalize(request.phone())
                .orElseThrow(() -> new BadRequestException(ErrorCodes.INVALID_PHONE));
        AppUser user = userRepository.findByPhoneE164(phone).orElseGet(() ->
                AppUser.builder().phoneE164(phone).status(UserStatus.ACTIVE).build());
        boolean created = user.getId() == null;
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.setDisplayName(request.displayName().trim());
        }
        user = userRepository.save(user);
        log.info("[ADMIN] {} app user {} ({}) by {}", created ? "created" : "password set for", user.getId(), phone, admin);
        return RowMappers.user(user);
    }

    @Transactional
    public void setPassword(Long userId, String password, String admin) {
        AppUser user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException(ErrorCodes.NOT_FOUND));
        user.setPasswordHash(passwordEncoder.encode(password));
        log.info("[ADMIN] password reset for user {} by {}", userId, admin);
    }

    @Transactional(readOnly = true)
    public UserDetailDto detail(Long userId) {
        AppUser user = require(userId);
        List<Rows.DeviceRow> devices = deviceRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(RowMappers::device).toList();
        List<Rows.NotificationRow> recent = notificationRepository.findTop20ByUserIdOrderByIdDesc(userId).stream()
                .map(RowMappers::notification).toList();
        return new UserDetailDto(RowMappers.user(user), devices, recent);
    }

    @Transactional
    public Rows.UserRow patch(Long userId, UserPatchRequest request, String admin) {
        AppUser user = require(userId);
        if (request.status() != null) {
            UserStatus status = UserStatus.valueOf(request.status());
            if (status != user.getStatus()) {
                user.setStatus(status);
                if (status == UserStatus.DISABLED) {
                    refreshTokenService.revokeAll(SubjectType.USER, userId);
                }
                log.info("[ADMIN] user {} status -> {} by {}", userId, status, admin);
            }
        }
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName().isBlank() ? null : request.displayName().trim());
        }
        return RowMappers.user(user);
    }

    /** Revokes every refresh token and deactivates every device; the user must log in again. */
    @Transactional
    public void forceLogout(Long userId, String admin) {
        require(userId);
        int revoked = refreshTokenService.revokeAll(SubjectType.USER, userId);
        int devices = deviceRepository.deactivateAllForUser(userId, Instant.now());
        log.info("[ADMIN] force-logout user {} by {}: revokedTokens={} deactivatedDevices={}", userId, admin, revoked, devices);
    }

    @Transactional
    public void deleteDevice(Long deviceId, String admin) {
        UserDevice device = deviceRepository.findById(deviceId).orElseThrow(() -> new NotFoundException(ErrorCodes.NOT_FOUND));
        refreshTokenService.revokeDevice(device.getUser().getId(), device.getDeviceId());
        deviceRepository.delete(device);
        log.info("[ADMIN] deleted device {} ({}) of user {} by {}", deviceId, device.getDeviceId(), device.getUser().getId(), admin);
    }

    private AppUser require(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException(ErrorCodes.NOT_FOUND));
    }
}

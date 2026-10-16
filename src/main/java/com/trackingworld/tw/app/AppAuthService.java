package com.trackingworld.tw.app;

import com.trackingworld.tw.alerts.domain.AppUser;
import com.trackingworld.tw.alerts.domain.RefreshToken;
import com.trackingworld.tw.alerts.domain.SubjectType;
import com.trackingworld.tw.alerts.domain.UserDevice;
import com.trackingworld.tw.alerts.domain.UserStatus;
import com.trackingworld.tw.alerts.repo.AppUserRepository;
import com.trackingworld.tw.alerts.repo.UserDeviceRepository;
import com.trackingworld.tw.alerts.service.AppSettingService;
import com.trackingworld.tw.app.dto.AuthResponse;
import com.trackingworld.tw.app.dto.DeviceInfo;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.exceptions.BadRequestException;
import com.trackingworld.tw.common.exceptions.ForbiddenException;
import com.trackingworld.tw.common.exceptions.UnauthorizedException;
import com.trackingworld.tw.common.util.PhoneNormalizer;
import com.trackingworld.tw.firebase.FirebaseTokenVerifier;
import com.trackingworld.tw.security.AuthProperties;
import com.trackingworld.tw.security.JwtService;
import com.trackingworld.tw.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** SPEC §7.2 app auth: Firebase phone login, dev login, refresh rotation, logout. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppAuthService {

    private final FirebaseTokenVerifier firebaseTokenVerifier;
    private final AppUserRepository userRepository;
    private final UserDeviceRepository deviceRepository;
    private final AppSettingService settings;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthProperties authProperties;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse firebaseLogin(String idToken, DeviceInfo device) {
        String firebasePhone = firebaseTokenVerifier.verifyPhone(idToken);
        String phone = PhoneNormalizer.normalize(firebasePhone)
                .orElseThrow(() -> new BadRequestException(ErrorCodes.INVALID_PHONE));
        log.info("[AUTH] firebase login phone={} device={}", phone, device.deviceId());
        return login(phone, device);
    }

    /** Phone + password sign-in. The user must have been created (with a password) from the dashboard / admin API. */
    @Transactional
    public AuthResponse passwordLogin(String rawPhone, String password, DeviceInfo device) {
        String phone = PhoneNormalizer.normalize(rawPhone)
                .orElseThrow(() -> new BadRequestException(ErrorCodes.INVALID_PHONE));
        AppUser user = userRepository.findByPhoneE164(phone)
                .orElseThrow(() -> new UnauthorizedException(ErrorCodes.INVALID_CREDENTIALS));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("[AUTH] bad password for {}", phone);
            throw new UnauthorizedException(ErrorCodes.INVALID_CREDENTIALS);
        }
        log.info("[AUTH] password login phone={} device={}", phone, device.deviceId());
        return login(phone, device);
    }

    @Transactional
    public AuthResponse devLogin(String rawPhone, DeviceInfo device) {
        if (!authProperties.devLoginEnabled()) {
            throw new ForbiddenException(ErrorCodes.DEV_LOGIN_DISABLED);
        }
        String phone = PhoneNormalizer.normalize(rawPhone)
                .orElseThrow(() -> new BadRequestException(ErrorCodes.INVALID_PHONE));
        log.warn("[AUTH] DEV login phone={} device={}", phone, device.deviceId());
        return login(phone, device);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken consumed = refreshTokenService.consume(refreshToken, SubjectType.USER);
        AppUser user = userRepository.findById(consumed.getSubjectId())
                .orElseThrow(() -> new UnauthorizedException(ErrorCodes.INVALID_REFRESH_TOKEN));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException(ErrorCodes.USER_DISABLED);
        }
        String deviceId = consumed.getDeviceId();
        UserDevice device = deviceRepository.findByUserIdAndDeviceId(user.getId(), deviceId)
                .orElseThrow(() -> new UnauthorizedException(ErrorCodes.INVALID_REFRESH_TOKEN));
        if (!device.isActive()) {
            throw new UnauthorizedException(ErrorCodes.INVALID_REFRESH_TOKEN);
        }
        device.setLastSeenAt(Instant.now());
        log.info("[AUTH] refresh user={} device={}", user.getId(), deviceId);
        return issue(user, deviceId);
    }

    @Transactional
    public void logout(Long userId, String deviceId) {
        deviceRepository.findByUserIdAndDeviceId(userId, deviceId).ifPresent(device -> {
            device.setActive(false);
            device.setFcmToken(null);
        });
        int revoked = refreshTokenService.revokeDevice(userId, deviceId);
        log.info("[AUTH] logout user={} device={} revokedRefreshTokens={}", userId, deviceId, revoked);
    }

    private AuthResponse login(String phone, DeviceInfo info) {
        Instant now = Instant.now();
        AppUser user = userRepository.findByPhoneE164(phone).orElseGet(() -> {
            log.info("[AUTH] creating app_user for {}", phone);
            return userRepository.save(AppUser.builder().phoneE164(phone).status(UserStatus.ACTIVE).build());
        });
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("[AUTH] login refused, user {} is {}", user.getId(), user.getStatus());
            throw new ForbiddenException(ErrorCodes.USER_DISABLED);
        }
        user.setLastLoginAt(now);

        UserDevice device = deviceRepository.findByUserIdAndDeviceId(user.getId(), info.deviceId())
                .orElseGet(() -> UserDevice.builder().user(user).deviceId(info.deviceId()).build());
        if (info.fcmToken() != null && !info.fcmToken().isBlank()) {
            device.setFcmToken(info.fcmToken().trim());
        }
        if (hasText(info.manufacturer())) {
            device.setManufacturer(info.manufacturer().trim());
        }
        if (hasText(info.model())) {
            device.setModel(info.model().trim());
        }
        if (hasText(info.osVersion())) {
            device.setOsVersion(info.osVersion().trim());
        }
        if (hasText(info.appVersion())) {
            device.setAppVersion(info.appVersion().trim());
        }
        device.setActive(true);
        device.setLastSeenAt(now);
        device.setUpdatedAt(now);
        device = deviceRepository.save(device);
        if (device.getFcmToken() != null) {
            deviceRepository.clearTokenElsewhere(device.getFcmToken(), device.getId(), now);
        }
        enforceDeviceLimit(user, device);
        return issue(user, device.getDeviceId());
    }

    private void enforceDeviceLimit(AppUser user, UserDevice current) {
        int max = settings.maxDevicesPerUser();
        List<UserDevice> active = deviceRepository.findByUserIdAndActiveTrueOrderByLastSeenAtAsc(user.getId());
        int excess = active.size() - max;
        if (excess <= 0) {
            return;
        }
        for (UserDevice candidate : active) {
            if (excess == 0) {
                break;
            }
            if (candidate.getId().equals(current.getId())) {
                continue;
            }
            candidate.setActive(false);
            candidate.setFcmToken(null);
            refreshTokenService.revokeDevice(user.getId(), candidate.getDeviceId());
            log.info("[AUTH] user {} exceeded max_devices_per_user={} -> deactivated oldest device {}",
                    user.getId(), max, candidate.getDeviceId());
            excess--;
        }
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private AuthResponse issue(AppUser user, String deviceId) {
        String access = jwtService.issueUserToken(user.getId(), deviceId);
        String refresh = refreshTokenService.issue(SubjectType.USER, user.getId(), deviceId);
        return new AuthResponse(access, jwtService.accessTtlSeconds(), refresh,
                new AuthResponse.UserDto(user.getId(), user.getPhoneE164(), user.getDisplayName(), user.getCreatedAt()));
    }
}

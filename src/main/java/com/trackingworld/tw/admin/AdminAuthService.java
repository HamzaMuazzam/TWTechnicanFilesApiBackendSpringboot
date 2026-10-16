package com.trackingworld.tw.admin;

import com.trackingworld.tw.admin.dto.AdminAuthResponse;
import com.trackingworld.tw.admin.dto.AdminDto;
import com.trackingworld.tw.alerts.domain.AdminUser;
import com.trackingworld.tw.alerts.domain.RefreshToken;
import com.trackingworld.tw.alerts.domain.SubjectType;
import com.trackingworld.tw.alerts.repo.AdminUserRepository;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.exceptions.NotFoundException;
import com.trackingworld.tw.common.exceptions.UnauthorizedException;
import com.trackingworld.tw.security.AdminPrincipal;
import com.trackingworld.tw.security.JwtService;
import com.trackingworld.tw.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminUserRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AdminAuthResponse login(String username, String password, String ip) {
        AdminUser admin = adminRepository.findByUsernameIgnoreCase(username.trim()).orElse(null);
        if (admin == null || !admin.isActive() || !passwordEncoder.matches(password, admin.getPasswordHash())) {
            log.warn("[AUTH][ADMIN] login failed username='{}' from {}", username, ip);
            throw new UnauthorizedException(ErrorCodes.INVALID_CREDENTIALS);
        }
        admin.setLastLoginAt(Instant.now());
        log.info("[AUTH][ADMIN] login ok username='{}' role={} from {}", admin.getUsername(), admin.getRole(), ip);
        return issue(admin);
    }

    @Transactional
    public AdminAuthResponse refresh(String refreshToken) {
        RefreshToken consumed = refreshTokenService.consume(refreshToken, SubjectType.ADMIN);
        AdminUser admin = adminRepository.findById(consumed.getSubjectId())
                .filter(AdminUser::isActive)
                .orElseThrow(() -> new UnauthorizedException(ErrorCodes.INVALID_REFRESH_TOKEN));
        return issue(admin);
    }

    @Transactional(readOnly = true)
    public AdminDto me(AdminPrincipal principal) {
        return adminRepository.findById(principal.id()).map(AdminAuthService::toDto)
                // open-access mode (app.security.enabled=false): no real admin row behind the principal
                .orElse(new AdminDto(principal.id(), principal.username(), principal.role().name(), true, null, null));
    }

    private AdminAuthResponse issue(AdminUser admin) {
        String access = jwtService.issueAdminToken(admin.getId(), admin.getUsername(), admin.getRole());
        String refresh = refreshTokenService.issue(SubjectType.ADMIN, admin.getId(), null);
        return new AdminAuthResponse(access, jwtService.accessTtlSeconds(), refresh, toDto(admin));
    }

    static AdminDto toDto(AdminUser a) {
        return new AdminDto(a.getId(), a.getUsername(), a.getRole().name(), a.isActive(), a.getCreatedAt(), a.getLastLoginAt());
    }
}

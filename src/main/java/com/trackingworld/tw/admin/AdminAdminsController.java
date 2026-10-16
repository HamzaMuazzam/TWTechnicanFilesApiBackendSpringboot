package com.trackingworld.tw.admin;

import com.trackingworld.tw.admin.dto.AdminCreateRequest;
import com.trackingworld.tw.admin.dto.AdminDto;
import com.trackingworld.tw.admin.dto.AdminPatchRequest;
import com.trackingworld.tw.alerts.domain.AdminRole;
import com.trackingworld.tw.alerts.domain.AdminUser;
import com.trackingworld.tw.alerts.domain.SubjectType;
import com.trackingworld.tw.alerts.repo.AdminUserRepository;
import com.trackingworld.tw.common.ApiPaths;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.ResponseWrapper;
import com.trackingworld.tw.common.exceptions.BadRequestException;
import com.trackingworld.tw.common.exceptions.ConflictException;
import com.trackingworld.tw.common.exceptions.NotFoundException;
import com.trackingworld.tw.security.AdminPrincipal;
import com.trackingworld.tw.security.RefreshTokenService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin account management - ADMIN role only. */
@Slf4j
@RestController
@RequestMapping(ApiPaths.ADMIN + "/admins")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Admin accounts")
public class AdminAdminsController {

    private final AdminUserRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseWrapper<List<AdminDto>> list() {
        return ResponseWrapper.success(adminRepository.findAll(Sort.by("id")).stream().map(AdminAuthService::toDto).toList());
    }

    @PostMapping
    @Transactional
    public ResponseWrapper<AdminDto> create(@Valid @RequestBody AdminCreateRequest request,
                                            @AuthenticationPrincipal AdminPrincipal principal) {
        String username = request.username().trim();
        if (adminRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException(ErrorCodes.ADMIN_EXISTS);
        }
        AdminUser admin = adminRepository.save(AdminUser.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(AdminRole.valueOf(request.role()))
                .active(true)
                .build());
        log.info("[ADMIN] admin '{}' ({}) created by {}", username, admin.getRole(), principal.username());
        return ResponseWrapper.success(AdminAuthService.toDto(admin));
    }

    @PatchMapping("/{id}")
    @Transactional
    public ResponseWrapper<AdminDto> patch(@PathVariable Long id, @Valid @RequestBody AdminPatchRequest request,
                                           @AuthenticationPrincipal AdminPrincipal principal) {
        AdminUser admin = require(id);
        boolean self = admin.getId().equals(principal.id());
        if (request.password() != null) {
            admin.setPasswordHash(passwordEncoder.encode(request.password()));
            if (!self) {
                refreshTokenService.revokeAll(SubjectType.ADMIN, id);
            }
        }
        if (request.role() != null) {
            AdminRole role = AdminRole.valueOf(request.role());
            if (role != admin.getRole() && admin.getRole() == AdminRole.ADMIN) {
                ensureNotLastAdmin(admin);
            }
            admin.setRole(role);
        }
        if (request.active() != null && request.active() != admin.isActive()) {
            if (!request.active()) {
                if (self) {
                    throw new BadRequestException(ErrorCodes.CANNOT_DELETE_SELF);
                }
                if (admin.getRole() == AdminRole.ADMIN) {
                    ensureNotLastAdmin(admin);
                }
                refreshTokenService.revokeAll(SubjectType.ADMIN, id);
            }
            admin.setActive(request.active());
        }
        log.info("[ADMIN] admin '{}' patched by {} (password={}, role={}, active={})", admin.getUsername(),
                principal.username(), request.password() != null, request.role(), request.active());
        return ResponseWrapper.success(AdminAuthService.toDto(admin));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseWrapper<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AdminPrincipal principal) {
        AdminUser admin = require(id);
        if (admin.getId().equals(principal.id())) {
            throw new BadRequestException(ErrorCodes.CANNOT_DELETE_SELF);
        }
        if (admin.getRole() == AdminRole.ADMIN && admin.isActive()) {
            ensureNotLastAdmin(admin);
        }
        refreshTokenService.revokeAll(SubjectType.ADMIN, id);
        adminRepository.delete(admin);
        log.info("[ADMIN] admin '{}' deleted by {}", admin.getUsername(), principal.username());
        return ResponseWrapper.success(null);
    }

    private void ensureNotLastAdmin(AdminUser admin) {
        if (admin.isActive() && adminRepository.countByRoleAndActiveTrue(AdminRole.ADMIN) <= 1) {
            throw new ConflictException(ErrorCodes.LAST_ADMIN);
        }
    }

    private AdminUser require(Long id) {
        return adminRepository.findById(id).orElseThrow(() -> new NotFoundException(ErrorCodes.NOT_FOUND));
    }
}

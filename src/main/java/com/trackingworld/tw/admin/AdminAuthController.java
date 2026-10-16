package com.trackingworld.tw.admin;

import com.trackingworld.tw.admin.dto.AdminAuthResponse;
import com.trackingworld.tw.admin.dto.AdminDto;
import com.trackingworld.tw.admin.dto.AdminLoginRequest;
import com.trackingworld.tw.admin.dto.AdminRefreshRequest;
import com.trackingworld.tw.common.ApiPaths;
import com.trackingworld.tw.common.ResponseWrapper;
import com.trackingworld.tw.common.web.ClientIp;
import com.trackingworld.tw.security.AdminPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admin - Auth")
public class AdminAuthController {

    private final AdminAuthService authService;

    @PostMapping(ApiPaths.ADMIN_AUTH + "/login")
    public ResponseWrapper<AdminAuthResponse> login(@Valid @RequestBody AdminLoginRequest request, HttpServletRequest http) {
        return ResponseWrapper.success(authService.login(request.username(), request.password(), ClientIp.of(http)));
    }

    @PostMapping(ApiPaths.ADMIN_AUTH + "/refresh")
    public ResponseWrapper<AdminAuthResponse> refresh(@Valid @RequestBody AdminRefreshRequest request) {
        return ResponseWrapper.success(authService.refresh(request.refreshToken()));
    }

    @GetMapping(ApiPaths.ADMIN + "/me")
    public ResponseWrapper<AdminDto> me(@AuthenticationPrincipal AdminPrincipal principal) {
        return ResponseWrapper.success(authService.me(principal));
    }
}

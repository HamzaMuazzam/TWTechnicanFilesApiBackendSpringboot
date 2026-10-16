package com.trackingworld.tw.app;

import com.trackingworld.tw.app.dto.AuthResponse;
import com.trackingworld.tw.app.dto.DevLoginRequest;
import com.trackingworld.tw.app.dto.PasswordLoginRequest;
import com.trackingworld.tw.app.dto.FirebaseLoginRequest;
import com.trackingworld.tw.app.dto.LogoutRequest;
import com.trackingworld.tw.app.dto.RefreshRequest;
import com.trackingworld.tw.common.ApiPaths;
import com.trackingworld.tw.common.ResponseWrapper;
import com.trackingworld.tw.security.AppUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.APP_AUTH)
@RequiredArgsConstructor
@Tag(name = "App - Auth")
public class AppAuthController {

    private final AppAuthService authService;

    @Operation(summary = "Login with a Firebase phone-auth ID token")
    @PostMapping("/firebase")
    public ResponseWrapper<AuthResponse> firebase(@Valid @RequestBody FirebaseLoginRequest request) {
        return ResponseWrapper.success(authService.firebaseLogin(request.idToken(), request.device()));
    }

    @Operation(summary = "Sign in with phone + password (user created from the dashboard / admin API)")
    @PostMapping("/login")
    public ResponseWrapper<AuthResponse> login(@Valid @RequestBody PasswordLoginRequest request) {
        return ResponseWrapper.success(authService.passwordLogin(request.phone(), request.password(), request.device()));
    }

    @Operation(summary = "Dev login by phone (only when auth.dev-login-enabled=true)")
    @PostMapping("/dev-login")
    public ResponseWrapper<AuthResponse> devLogin(@Valid @RequestBody DevLoginRequest request) {
        return ResponseWrapper.success(authService.devLogin(request.phone(), request.device()));
    }

    @Operation(summary = "Rotate refresh token")
    @PostMapping("/refresh")
    public ResponseWrapper<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseWrapper.success(authService.refresh(request.refreshToken()));
    }

    @Operation(summary = "Logout this device (Bearer)")
    @PostMapping("/logout")
    public ResponseWrapper<Void> logout(@Valid @RequestBody LogoutRequest request,
                                        @AuthenticationPrincipal AppUserPrincipal principal) {
        authService.logout(principal.userId(), request.deviceId());
        return ResponseWrapper.success(null);
    }
}

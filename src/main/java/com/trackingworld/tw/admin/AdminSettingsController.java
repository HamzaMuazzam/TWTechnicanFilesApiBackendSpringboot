package com.trackingworld.tw.admin;

import com.trackingworld.tw.alerts.service.AppSettingService;
import com.trackingworld.tw.common.ApiPaths;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.ResponseWrapper;
import com.trackingworld.tw.common.exceptions.BadRequestException;
import com.trackingworld.tw.security.AdminPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping(ApiPaths.ADMIN + "/settings")
@RequiredArgsConstructor
@Tag(name = "Admin - Settings")
public class AdminSettingsController {

    private final AppSettingService settings;

    @GetMapping
    public ResponseWrapper<Map<String, String>> get() {
        return ResponseWrapper.success(settings.all());
    }

    @Operation(summary = "Update settings: body is a {key: value} map (retention_days, max_devices_per_user, dispatch_max_attempts)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseWrapper<Map<String, String>> put(@RequestBody Map<String, String> body,
                                                    @AuthenticationPrincipal AdminPrincipal principal) {
        if (body == null || body.isEmpty() || body.size() > 50) {
            throw new BadRequestException(ErrorCodes.INVALID_SETTING);
        }
        log.info("[ADMIN] settings updated by {}: {}", principal.username(), body.keySet());
        return ResponseWrapper.success(settings.update(body));
    }
}

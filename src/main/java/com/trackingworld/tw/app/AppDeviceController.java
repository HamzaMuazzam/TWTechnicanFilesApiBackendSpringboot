package com.trackingworld.tw.app;

import com.trackingworld.tw.app.dto.DeviceDto;
import com.trackingworld.tw.app.dto.HeartbeatRequest;
import com.trackingworld.tw.app.dto.TokenUpdateRequest;
import com.trackingworld.tw.common.ApiPaths;
import com.trackingworld.tw.common.ResponseWrapper;
import com.trackingworld.tw.security.AppUserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.APP_DEVICES)
@RequiredArgsConstructor
@Tag(name = "App - Devices")
public class AppDeviceController {

    private final AppDeviceService deviceService;

    @PutMapping("/token")
    public ResponseWrapper<Void> token(@Valid @RequestBody TokenUpdateRequest request,
                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        deviceService.updateToken(principal.userId(), request.deviceId(), request.fcmToken());
        return ResponseWrapper.success(null);
    }

    @PostMapping("/heartbeat")
    public ResponseWrapper<Void> heartbeat(@Valid @RequestBody HeartbeatRequest request,
                                           @AuthenticationPrincipal AppUserPrincipal principal) {
        deviceService.heartbeat(principal.userId(), request);
        return ResponseWrapper.success(null);
    }

    @GetMapping
    public ResponseWrapper<List<DeviceDto>> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseWrapper.success(deviceService.list(principal.userId(), principal.deviceId()));
    }

    @DeleteMapping("/{deviceId}")
    public ResponseWrapper<Void> delete(@PathVariable String deviceId, @AuthenticationPrincipal AppUserPrincipal principal) {
        deviceService.delete(principal.userId(), deviceId);
        return ResponseWrapper.success(null);
    }
}

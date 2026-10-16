package com.trackingworld.tw.app;

import com.trackingworld.tw.app.dto.AckRequest;
import com.trackingworld.tw.app.dto.AlertTypeDto;
import com.trackingworld.tw.app.dto.CountDto;
import com.trackingworld.tw.app.dto.NotificationDto;
import com.trackingworld.tw.app.dto.NotificationPageDto;
import com.trackingworld.tw.app.dto.ReadAllRequest;
import com.trackingworld.tw.app.dto.ReadRequest;
import com.trackingworld.tw.app.dto.UpdatedDto;
import com.trackingworld.tw.common.ApiPaths;
import com.trackingworld.tw.common.ResponseWrapper;
import com.trackingworld.tw.security.AppUserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "App - Notifications")
public class AppNotificationController {

    private final AppNotificationService service;

    @GetMapping(ApiPaths.APP_NOTIFICATIONS)
    public ResponseWrapper<NotificationPageDto> list(@RequestParam(required = false) Long cursor,
                                                     @RequestParam(required = false, defaultValue = "30") Integer size,
                                                     @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
                                                     @RequestParam(required = false) String alertType,
                                                     @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseWrapper.success(service.list(principal.userId(), cursor, size, unreadOnly, alertType));
    }

    @GetMapping(ApiPaths.APP_NOTIFICATIONS + "/unread-count")
    public ResponseWrapper<CountDto> unreadCount(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseWrapper.success(new CountDto(service.unreadCount(principal.userId())));
    }

    @GetMapping(ApiPaths.APP_NOTIFICATIONS + "/{id}")
    public ResponseWrapper<NotificationDto> get(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseWrapper.success(service.get(principal.userId(), id));
    }

    @PostMapping(ApiPaths.APP_NOTIFICATIONS + "/ack")
    public ResponseWrapper<UpdatedDto> ack(@Valid @RequestBody AckRequest request,
                                           @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseWrapper.success(new UpdatedDto(service.ack(principal.userId(), request)));
    }

    @PostMapping(ApiPaths.APP_NOTIFICATIONS + "/read")
    public ResponseWrapper<UpdatedDto> read(@Valid @RequestBody ReadRequest request,
                                            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseWrapper.success(new UpdatedDto(service.markRead(principal.userId(), request.ids())));
    }

    @PostMapping(ApiPaths.APP_NOTIFICATIONS + "/read-all")
    public ResponseWrapper<UpdatedDto> readAll(@RequestBody(required = false) ReadAllRequest request,
                                               @AuthenticationPrincipal AppUserPrincipal principal) {
        Long beforeId = request == null ? null : request.beforeId();
        return ResponseWrapper.success(new UpdatedDto(service.markAllRead(principal.userId(), beforeId)));
    }

    @GetMapping(ApiPaths.APP_ALERT_TYPES)
    public ResponseWrapper<List<AlertTypeDto>> alertTypes() {
        return ResponseWrapper.success(service.alertTypes());
    }
}

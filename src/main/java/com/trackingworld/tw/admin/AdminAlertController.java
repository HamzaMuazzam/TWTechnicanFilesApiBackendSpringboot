package com.trackingworld.tw.admin;

import com.trackingworld.tw.admin.dto.AlertDetailDto;
import com.trackingworld.tw.admin.dto.ResendResultDto;
import com.trackingworld.tw.common.ApiPaths;
import com.trackingworld.tw.common.ResponseWrapper;
import com.trackingworld.tw.security.AdminPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admin - Alerts")
public class AdminAlertController {

    private final AdminAlertService alertService;

    @GetMapping(ApiPaths.ADMIN + "/alerts/{id}")
    public ResponseWrapper<AlertDetailDto> detail(@PathVariable Long id) {
        return ResponseWrapper.success(alertService.detail(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(ApiPaths.ADMIN + "/notifications/{id}/resend")
    public ResponseWrapper<ResendResultDto> resend(@PathVariable Long id, @AuthenticationPrincipal AdminPrincipal principal) {
        return ResponseWrapper.success(alertService.resend(id, principal.username()));
    }
}

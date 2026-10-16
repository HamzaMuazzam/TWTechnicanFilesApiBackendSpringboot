package com.trackingworld.tw.admin;

import com.trackingworld.tw.admin.dto.Rows;
import com.trackingworld.tw.admin.dto.UserDetailDto;
import com.trackingworld.tw.admin.dto.UserCreateRequest;
import com.trackingworld.tw.admin.dto.UserPasswordRequest;
import com.trackingworld.tw.admin.dto.UserPatchRequest;
import com.trackingworld.tw.common.ApiPaths;
import com.trackingworld.tw.common.ResponseWrapper;
import com.trackingworld.tw.security.AdminPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admin - Users & Devices")
public class AdminUserController {

    private final AdminUserService userService;

    @Operation(summary = "Create an app user with phone + password (or reset the password if the phone exists)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(ApiPaths.ADMIN + "/users")
    public ResponseWrapper<Rows.UserRow> create(@Valid @RequestBody UserCreateRequest request,
                                                @AuthenticationPrincipal AdminPrincipal principal) {
        return ResponseWrapper.success(userService.create(request, principal.username()));
    }

    @Operation(summary = "Set a new password for an app user")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(ApiPaths.ADMIN + "/users/{id}/password")
    public ResponseWrapper<Void> setPassword(@PathVariable Long id, @Valid @RequestBody UserPasswordRequest request,
                                             @AuthenticationPrincipal AdminPrincipal principal) {
        userService.setPassword(id, request.password(), principal.username());
        return ResponseWrapper.success(null);
    }

    @GetMapping(ApiPaths.ADMIN + "/users/{id}")
    public ResponseWrapper<UserDetailDto> detail(@PathVariable Long id) {
        return ResponseWrapper.success(userService.detail(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping(ApiPaths.ADMIN + "/users/{id}")
    public ResponseWrapper<Rows.UserRow> patch(@PathVariable Long id, @Valid @RequestBody UserPatchRequest request,
                                               @AuthenticationPrincipal AdminPrincipal principal) {
        return ResponseWrapper.success(userService.patch(id, request, principal.username()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(ApiPaths.ADMIN + "/users/{id}/force-logout")
    public ResponseWrapper<Void> forceLogout(@PathVariable Long id, @AuthenticationPrincipal AdminPrincipal principal) {
        userService.forceLogout(id, principal.username());
        return ResponseWrapper.success(null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(ApiPaths.ADMIN + "/devices/{id}")
    public ResponseWrapper<Void> deleteDevice(@PathVariable Long id, @AuthenticationPrincipal AdminPrincipal principal) {
        userService.deleteDevice(id, principal.username());
        return ResponseWrapper.success(null);
    }
}

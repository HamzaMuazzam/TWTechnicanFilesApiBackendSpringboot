package com.trackingworld.tw.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** App sign-in: phone (any PK format) + password. */
public record PasswordLoginRequest(@NotBlank @Size(max = 40) String phone,
                                   @NotBlank @Size(max = 200) String password,
                                   @NotNull @Valid DeviceInfo device) {
}

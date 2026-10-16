package com.trackingworld.tw.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FirebaseLoginRequest(@NotBlank String idToken, @NotNull @Valid DeviceInfo device) {
}

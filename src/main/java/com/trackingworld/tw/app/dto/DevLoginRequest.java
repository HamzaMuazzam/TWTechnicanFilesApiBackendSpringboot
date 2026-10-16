package com.trackingworld.tw.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DevLoginRequest(@NotBlank @Size(max = 40) String phone, @NotNull @Valid DeviceInfo device) {
}

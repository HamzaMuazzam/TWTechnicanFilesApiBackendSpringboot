package com.trackingworld.tw.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminRefreshRequest(@NotBlank String refreshToken) {
}

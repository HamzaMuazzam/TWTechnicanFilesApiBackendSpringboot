package com.trackingworld.tw.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SqlRequest(@NotBlank @Size(max = 20_000) String sql) {
}

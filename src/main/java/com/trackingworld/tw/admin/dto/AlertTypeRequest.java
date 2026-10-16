package com.trackingworld.tw.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AlertTypeRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = "[A-Za-z0-9_\\-.]+", message = "letters, digits, _ - . only") String code,
        @NotBlank @Size(max = 200) String label,
        @NotBlank @Pattern(regexp = "INFO|WARNING|CRITICAL") String severity,
        @Size(max = 100) String icon,
        @Size(max = 20) String color,
        @Size(max = 100) String channelId,
        Boolean active,
        Integer sortOrder
) {
}

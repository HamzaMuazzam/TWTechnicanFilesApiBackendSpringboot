package com.trackingworld.tw.admin.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminPatchRequest(
        @Size(min = 8, max = 200) String password,
        @Pattern(regexp = "ADMIN|VIEWER") String role,
        Boolean active
) {
}

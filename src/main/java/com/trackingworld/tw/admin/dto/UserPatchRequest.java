package com.trackingworld.tw.admin.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserPatchRequest(@Pattern(regexp = "ACTIVE|DISABLED") String status, @Size(max = 200) String displayName) {
}

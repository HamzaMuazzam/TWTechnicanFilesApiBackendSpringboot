package com.trackingworld.tw.admin.dto;

import jakarta.validation.constraints.NotNull;

public record SourcePatchRequest(@NotNull Boolean active) {
}

package com.trackingworld.tw.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SourceCreateRequest(@NotBlank @Size(max = 100) String name) {
}

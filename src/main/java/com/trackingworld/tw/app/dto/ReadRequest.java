package com.trackingworld.tw.app.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReadRequest(@NotEmpty @Size(max = 500) List<Long> ids) {
}

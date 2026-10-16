package com.trackingworld.tw.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserPasswordRequest(@NotBlank @Size(min = 4, max = 200) String password) {
}

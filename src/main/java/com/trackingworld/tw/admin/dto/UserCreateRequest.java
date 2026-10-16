package com.trackingworld.tw.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create an app user (phone + password) from the dashboard / Swagger. */
public record UserCreateRequest(@NotBlank @Size(max = 40) String phone,
                                @NotBlank @Size(min = 4, max = 200) String password,
                                @Size(max = 200) String displayName) {
}

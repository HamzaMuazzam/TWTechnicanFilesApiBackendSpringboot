package com.trackingworld.tw.admin.dto;

public record AdminAuthResponse(String accessToken, long accessExpiresInSec, String refreshToken, AdminDto admin) {
}

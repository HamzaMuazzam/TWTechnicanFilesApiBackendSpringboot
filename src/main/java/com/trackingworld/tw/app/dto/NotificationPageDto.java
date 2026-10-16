package com.trackingworld.tw.app.dto;

import java.util.List;

public record NotificationPageDto(List<NotificationDto> items, Long nextCursor) {
}

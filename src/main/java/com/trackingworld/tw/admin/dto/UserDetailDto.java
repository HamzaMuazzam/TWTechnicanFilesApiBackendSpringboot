package com.trackingworld.tw.admin.dto;

import java.util.List;

public record UserDetailDto(Rows.UserRow user, List<Rows.DeviceRow> devices, List<Rows.NotificationRow> recentNotifications) {
}

package com.trackingworld.tw.admin.dto;

import java.util.Map;

public record DispatchStatusDto(Map<String, Long> deliveriesByStatus, long dueNow, int inFlight,
                                Map<String, Long> notificationsByStatus, boolean firebaseEnabled) {
}

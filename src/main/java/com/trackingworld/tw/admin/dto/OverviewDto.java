package com.trackingworld.tw.admin.dto;

import java.util.List;

/** SPEC §7.5 GET /admin/overview. */
public record OverviewDto(
        long alertsTotal,
        long notificationsTotal,
        long sent,
        long delivered,
        long read,
        long failed,
        long noDevice,
        long discarded,
        long usersTotal,
        long usersNew,
        long devicesActive,
        long devicesSilent,
        double deliveryRatePct,
        double readRatePct,
        List<PerDay> perDay,
        List<ByType> byType
) {
    public record PerDay(String date, long alerts, long delivered, long read, long failed) {
    }

    public record ByType(String code, String label, long count) {
    }
}

package com.trackingworld.tw.alerts.service;

import com.trackingworld.tw.alerts.repo.AlertRepository;
import com.trackingworld.tw.alerts.repo.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/** Nightly 03:00 (app.timezone, default Asia/Karachi): delete alerts older than retention_days (cascades). */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetentionJob {

    private final AlertRepository alertRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AppSettingService settings;

    @Scheduled(cron = "0 0 3 * * *", zone = "${app.timezone}")
    @Transactional
    public void run() {
        int days = settings.retentionDays();
        Instant cutoff = Instant.now().minus(Duration.ofDays(days));
        long start = System.currentTimeMillis();
        int alerts = alertRepository.deleteOlderThan(cutoff);
        int tokens = refreshTokenRepository.purgeOlderThan(Instant.now().minus(Duration.ofDays(7)));
        log.info("[RETENTION] retention_days={} cutoff={} deletedAlerts={} purgedRefreshTokens={} in {} ms",
                days, cutoff, alerts, tokens, System.currentTimeMillis() - start);
    }
}
